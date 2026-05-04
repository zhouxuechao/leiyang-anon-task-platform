package com.leiyang.anontask.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.config.AppProperties;
import com.leiyang.anontask.domain.UserAccount;
import com.leiyang.anontask.domain.UserAuth;
import com.leiyang.anontask.domain.enums.AuthStatus;
import com.leiyang.anontask.domain.enums.UserStatus;
import com.leiyang.anontask.dto.mp.MpLoginRequest;
import com.leiyang.anontask.dto.mp.MpLoginResponse;
import com.leiyang.anontask.dto.mp.MpUserDto;
import com.leiyang.anontask.dto.mp.PhoneLoginRequest;
import com.leiyang.anontask.dto.mp.RealnameRequest;
import com.leiyang.anontask.dto.mp.WxLoginRequest;
import com.leiyang.anontask.repo.UserAccountRepository;
import com.leiyang.anontask.repo.UserAuthRepository;
import com.leiyang.anontask.security.AuthPrincipal;
import com.leiyang.anontask.security.AuthRole;
import com.leiyang.anontask.security.TokenService;
import jakarta.transaction.Transactional;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

@Service
public class MpAuthService {
  private static final String PHONE_OPENID_PREFIX = "phone:";
  private static final String EMAIL_OPENID_PREFIX = "email:";
  private static final String DEV_PHONE_CODE = "123456";

  private final AppProperties props;
  private final UserAccountRepository userRepo;
  private final UserAuthRepository authRepo;
  private final TokenService tokenService;
  private final CryptoService cryptoService;
  private final EmailCodeService emailCodeService;
  private final PasswordEncoder passwordEncoder;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  public MpAuthService(
      AppProperties props,
      UserAccountRepository userRepo,
      UserAuthRepository authRepo,
      TokenService tokenService,
      CryptoService cryptoService,
      EmailCodeService emailCodeService,
      PasswordEncoder passwordEncoder
  ) {
    this.props = props;
    this.userRepo = userRepo;
    this.authRepo = authRepo;
    this.tokenService = tokenService;
    this.cryptoService = cryptoService;
    this.emailCodeService = emailCodeService;
    this.passwordEncoder = passwordEncoder;
    this.objectMapper = new ObjectMapper();
    var rf = new SimpleClientHttpRequestFactory();
    rf.setConnectTimeout(5000);
    rf.setReadTimeout(5000);
    this.restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(rf));
  }

  @Transactional
  public MpLoginResponse mockLogin(MpLoginRequest req) {
    if (!props.mp().allowMockLogin()) {
      throw new BizException("Mock login is disabled");
    }
    UserAccount user = userRepo.findByOpenId(req.openId()).orElseGet(() -> {
      UserAccount u = new UserAccount();
      u.setOpenId(req.openId());
      u.setStatus(UserStatus.ACTIVE);
      u.setCreditScore(100);
      u.setNickname(req.nickname());
      u.setAvatar(defaultAvatar(req.openId(), req.avatar()));
      return userRepo.save(u);
    });
    if (req.nickname() != null && !req.nickname().isBlank()) {
      user.setNickname(req.nickname());
    }
    if (req.avatar() != null && !req.avatar().isBlank()) {
      user.setAvatar(req.avatar());
    }
    userRepo.save(user);

    String token = tokenService.issueToken(new AuthPrincipal(user.getId(), AuthRole.MP_USER));
    return new MpLoginResponse(token, toDto(user));
  }

  @Transactional
  public MpLoginResponse wxLogin(WxLoginRequest req) {
    String appId = trim(props.mp().appId());
    String appSecret = trim(props.mp().appSecret());
    if (isInvalidWxConfig(appId) || isInvalidWxConfig(appSecret)) {
      throw new BizException("WeChat login is not configured: set WX_MP_APP_ID and WX_MP_APP_SECRET");
    }
    WxSessionResp wx = fetchSession(appId, appSecret, req.code());
    if (wx.openid == null || wx.openid.isBlank()) {
      throw new BizException("WeChat login failed: no openid");
    }
    UserAccount user = userRepo.findByOpenId(wx.openid).orElseGet(() -> {
      UserAccount u = new UserAccount();
      u.setOpenId(wx.openid);
      u.setStatus(UserStatus.ACTIVE);
      u.setCreditScore(100);
      u.setNickname(defaultNickname(wx.openid));
      u.setAvatar(defaultAvatar(wx.openid, req.avatar()));
      return userRepo.save(u);
    });
    if (req.nickname() != null && !req.nickname().isBlank()) {
      user.setNickname(req.nickname().trim());
    } else if (safe(user.getNickname()).isBlank()) {
      user.setNickname(defaultNickname(wx.openid));
    }
    if (req.avatar() != null && !req.avatar().isBlank()) {
      user.setAvatar(req.avatar().trim());
    }
    userRepo.save(user);
    String phoneCode = trim(req.phoneCode());
    if (!phoneCode.isEmpty()) {
      bindMobile(user, fetchPhoneNumber(appId, appSecret, phoneCode));
    }

    String token = tokenService.issueToken(new AuthPrincipal(user.getId(), AuthRole.MP_USER));
    return new MpLoginResponse(token, toDto(user));
  }

  public void sendPhoneCode(String mobile) {
    // TODO: 接入短信服务后在这里发送真实验证码；本地开发默认验证码为 123456。
    if (!isValidMobile(mobile)) throw new BizException("mobile format invalid");
  }

  public void sendEmailCode(String email) {
    emailCodeService.send(email);
  }

  public boolean isEmailRegistered(String email) {
    String normalized = requireEmail(email);
    return authRepo.findByEmailIgnoreCase(normalized)
        .map(a -> !trim(a.getPasswordHash()).isEmpty())
        .orElse(false);
  }

  public void verifyEmailCode(String email, String code) {
    String normalized = requireEmail(email);
    emailCodeService.verify(normalized, code);
  }

  @Transactional
  public MpLoginResponse emailRegister(String email, String code, String password) {
    String normalized = requireEmail(email);
    String pwd = requirePassword(password);
    emailCodeService.consume(normalized, code);
    var existing = authRepo.findByEmailIgnoreCase(normalized).orElse(null);
    if (existing != null && existing.getPasswordHash() != null && !existing.getPasswordHash().isBlank()) {
      throw new BizException("email already registered");
    }
    UserAccount user = existing == null
        ? userRepo.findByOpenId(EMAIL_OPENID_PREFIX + normalized).orElseGet(() -> createEmailUser(normalized))
        : existing.getUser();
    UserAuth auth = existing == null ? authFor(user) : existing;
    auth.setEmail(normalized);
    auth.setPasswordHash(passwordEncoder.encode(pwd));
    authRepo.save(auth);
    return issueLogin(user);
  }

  @Transactional
  public MpLoginResponse emailCodeLogin(String email, String code) {
    String normalized = requireEmail(email);
    emailCodeService.consume(normalized, code);
    UserAuth auth = authRepo.findByEmailIgnoreCase(normalized)
        .orElseThrow(() -> new BizException("email not registered"));
    return issueLogin(auth.getUser());
  }

  @Transactional
  public MpLoginResponse emailPasswordLogin(String email, String password) {
    String normalized = requireEmail(email);
    String pwd = trim(password);
    if (pwd.isEmpty()) throw new BizException("password is required");
    UserAuth auth = authRepo.findByEmailIgnoreCase(normalized)
        .orElseThrow(() -> new BizException("email or password invalid"));
    if (trim(auth.getPasswordHash()).isEmpty() || !passwordEncoder.matches(pwd, auth.getPasswordHash())) {
      throw new BizException("email or password invalid");
    }
    return issueLogin(auth.getUser());
  }

  @Transactional
  public MpLoginResponse phoneLogin(PhoneLoginRequest req) {
    String mobile = trim(req.mobile());
    if (!isValidMobile(mobile)) throw new BizException("mobile format invalid");
    if (!DEV_PHONE_CODE.equals(trim(req.code()))) throw new BizException("verification code invalid");

    UserAccount user = authRepo.findByMobile(mobile)
        .map(UserAuth::getUser)
        .orElseGet(() -> userRepo.findByOpenId(PHONE_OPENID_PREFIX + mobile).orElseGet(() -> {
          UserAccount u = new UserAccount();
          u.setOpenId(PHONE_OPENID_PREFIX + mobile);
          u.setStatus(UserStatus.ACTIVE);
          u.setCreditScore(100);
          u.setNickname(defaultPhoneNickname(mobile));
          u.setAvatar(defaultAvatar(mobile, req.avatar()));
          return userRepo.save(u);
        }));
    if (req.nickname() != null && !req.nickname().isBlank()) {
      user.setNickname(req.nickname().trim());
    } else if (safe(user.getNickname()).isBlank()) {
      user.setNickname(defaultPhoneNickname(mobile));
    }
    if (req.avatar() != null && !req.avatar().isBlank()) {
      user.setAvatar(req.avatar().trim());
    }
    userRepo.save(user);
    bindMobile(user, mobile);

    String token = tokenService.issueToken(new AuthPrincipal(user.getId(), AuthRole.MP_USER));
    return new MpLoginResponse(token, toDto(user));
  }

  private UserAccount createEmailUser(String email) {
    UserAccount u = new UserAccount();
    u.setOpenId(EMAIL_OPENID_PREFIX + email);
    u.setStatus(UserStatus.ACTIVE);
    u.setCreditScore(100);
    u.setNickname(defaultEmailNickname(email));
    u.setAvatar(defaultAvatar(email, ""));
    return userRepo.save(u);
  }

  private UserAuth authFor(UserAccount user) {
    return authRepo.findByUser(user).orElseGet(() -> {
      UserAuth a = new UserAuth();
      a.setUser(user);
      a.setStatus(AuthStatus.UNVERIFIED);
      return a;
    });
  }

  private MpLoginResponse issueLogin(UserAccount user) {
    String token = tokenService.issueToken(new AuthPrincipal(user.getId(), AuthRole.MP_USER));
    return new MpLoginResponse(token, toDto(user));
  }

  @Transactional
  public void submitRealname(UserAccount user, RealnameRequest req) {
    UserAuth auth = authRepo.findByUser(user).orElseGet(() -> {
      UserAuth a = new UserAuth();
      a.setUser(user);
      a.setStatus(AuthStatus.UNVERIFIED);
      return a;
    });
    auth.setRealName(req.realName());
    auth.setIdNoEnc(cryptoService.encryptOrFallback(req.idNo()));
    auth.setMobile(req.mobile());
    auth.setPayAccount(req.payAccount());
    auth.setStatus(AuthStatus.VERIFIED);
    authRepo.save(auth);
  }

  @Transactional
  public MpUserDto updateMe(UserAccount user, String nickname, String avatar, String gender, String signature) {
    String nn = safe(nickname).trim();
    String av = safe(avatar).trim();
    String gd = normalizeGender(gender);
    String sg = safe(signature).trim();
    if (!nn.isEmpty()) {
      if (nn.length() > 64) nn = nn.substring(0, 64);
      user.setNickname(nn);
    }
    if (!av.isEmpty()) {
      if (av.length() > 512) av = av.substring(0, 512);
      user.setAvatar(av);
    }
    if (!gd.isEmpty()) {
      user.setGender(gd);
    }
    if (sg.length() > 255) sg = sg.substring(0, 255);
    user.setSignature(sg);
    userRepo.save(user);
    return toDto(user);
  }

  public MpUserDto toDto(UserAccount user) {
    return new MpUserDto(
        user.getId(),
        user.getOpenId(),
        user.getNickname(),
        user.getAvatar(),
        user.getGender(),
        user.getSignature(),
        user.getStatus().name(),
        user.getCreditScore()
    );
  }

  public ProfileDto profile(UserAccount user) {
    var auth = authRepo.findByUser(user).orElse(null);
    return new ProfileDto(
        auth == null ? "" : safe(auth.getRealName()),
        auth == null ? "" : maskIdNo(auth.getIdNoEnc()),
        auth == null ? "" : safe(auth.getMobile()),
        auth == null ? "" : safe(auth.getPayAccount())
    );
  }

  private WxSessionResp fetchSession(String appId, String appSecret, String code) {
    String url = "https://api.weixin.qq.com/sns/jscode2session"
        + "?appid=" + enc(appId)
        + "&secret=" + enc(appSecret)
        + "&js_code=" + enc(code)
        + "&grant_type=authorization_code";
    try {
      String raw = restTemplate.getForObject(url, String.class);
      Map<String, Object> m = parseWxBody(raw);
      if (m == null) throw new BizException("WeChat API returns empty response");
      int errcode = parseErrCode(m.get("errcode"));
      if (errcode != 0) {
        String errmsg = String.valueOf(m.getOrDefault("errmsg", "errcode=" + errcode));
        if (errcode == 41002) {
          throw new BizException("WeChat login failed: appid missing (check WX_MP_APP_ID)");
        }
        throw new BizException("WeChat login failed: " + errmsg);
      }
      return new WxSessionResp((String) m.get("openid"), (String) m.get("session_key"));
    } catch (BizException e) {
      throw e;
    } catch (ResourceAccessException e) {
      throw new BizException("WeChat login request failed: cannot reach api.weixin.qq.com (DNS/Network timeout)");
    } catch (Exception e) {
      String msg = e.getMessage();
      if (msg == null || msg.isBlank()) {
        throw new BizException("WeChat login request failed: unknown network error");
      }
      throw new BizException("WeChat login request failed: " + msg);
    }
  }

  private String fetchPhoneNumber(String appId, String appSecret, String phoneCode) {
    String code = trim(phoneCode);
    if (code.isEmpty()) {
      throw new BizException("WeChat phone authorization is required");
    }
    String accessToken = getWxAccessToken(appId, appSecret);
    try {
      return fetchPhoneNumberOnce(accessToken, code);
    } catch (BizException e) {
      String msg = safe(e.getMessage()).toLowerCase(Locale.ROOT);
      boolean retryableToken = msg.contains("access_token")
          || msg.contains("40001")
          || msg.contains("42001")
          || msg.contains("41001");
      if (retryableToken) {
        String fresh = getWxAccessToken(appId, appSecret);
        return fetchPhoneNumberOnce(fresh, code);
      }
      throw e;
    } catch (ResourceAccessException e) {
      throw new BizException("WeChat phone request failed: cannot reach api.weixin.qq.com (DNS/Network timeout)");
    } catch (HttpStatusCodeException e) {
      int status = e.getStatusCode().value();
      String body = trim(e.getResponseBodyAsString());
      if (status == 412) {
        // 412 在不同网络路径下可能是 code 失效，也可能是 access_token 侧异常；先强制刷新 token 重试一次
        try {
          String fresh = getWxAccessToken(appId, appSecret);
          return fetchPhoneNumberOnce(fresh, code);
        } catch (Exception ignored) {
          // continue to parse current response body
        }
        if (!body.isEmpty()) {
          try {
            Map<String, Object> m = parseWxBody(body);
            int errcode = parseErrCode(m.get("errcode"));
            String errmsg = String.valueOf(m.getOrDefault("errmsg", "HTTP 412"));
            throw new BizException("WeChat phone authorization failed: " + errmsg + " (errcode=" + errcode + ")");
          } catch (Exception ignore) {
            // fall through
          }
        }
        throw new BizException("WeChat phone authorization failed: HTTP 412 (possibly phoneCode expired/used or token mismatch), please retry");
      }
      if (!body.isEmpty()) {
        try {
          Map<String, Object> m = parseWxBody(body);
          int errcode = parseErrCode(m.get("errcode"));
          String errmsg = String.valueOf(m.getOrDefault("errmsg", "HTTP " + status));
          throw new BizException("WeChat phone authorization failed: " + errmsg + " (errcode=" + errcode + ")");
        } catch (Exception ignore) {
          // fall through
        }
      }
      throw new BizException("WeChat phone request failed: HTTP " + status);
    } catch (Exception e) {
      String msg = e.getMessage();
      if (msg == null || msg.isBlank()) {
        throw new BizException("WeChat phone request failed: unknown network error");
      }
      throw new BizException("WeChat phone request failed: " + msg);
    }
  }

  private String getWxAccessToken(String appId, String appSecret) {
    String url = "https://api.weixin.qq.com/cgi-bin/token"
        + "?grant_type=client_credential"
        + "&appid=" + enc(appId)
        + "&secret=" + enc(appSecret);
    try {
      String raw = restTemplate.getForObject(url, String.class);
      Map<String, Object> m = parseWxBody(raw);
      int errcode = parseErrCode(m.get("errcode"));
      if (errcode != 0) {
        String errmsg = String.valueOf(m.getOrDefault("errmsg", "errcode=" + errcode));
        throw new BizException("WeChat access_token failed: " + errmsg);
      }
      String token = trim(String.valueOf(m.getOrDefault("access_token", "")));
      if (token.isEmpty()) {
        throw new BizException("WeChat access_token failed: empty token");
      }
      return token;
    } catch (BizException e) {
      throw e;
    } catch (ResourceAccessException e) {
      throw new BizException("WeChat access_token request failed: cannot reach api.weixin.qq.com (DNS/Network timeout)");
    } catch (Exception e) {
      String msg = e.getMessage();
      if (msg == null || msg.isBlank()) {
        throw new BizException("WeChat access_token request failed: unknown network error");
      }
      throw new BizException("WeChat access_token request failed: " + msg);
    }
  }

  private String fetchPhoneNumberOnce(String accessToken, String code) {
    String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + enc(accessToken);
    Map<String, String> body = new HashMap<>();
    body.put("code", code);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
    String raw = restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
    Map<String, Object> m = parseWxBody(raw);
    int errcode = parseErrCode(m.get("errcode"));
    if (errcode != 0) {
      String errmsg = String.valueOf(m.getOrDefault("errmsg", "errcode=" + errcode));
      throw new BizException("WeChat phone authorization failed: " + errmsg + " (errcode=" + errcode + ")");
    }
    Map<String, Object> phoneInfo = asMap(m.get("phone_info"));
    String phone = trim(String.valueOf(phoneInfo.getOrDefault("phoneNumber", "")));
    if (phone.isEmpty()) {
      throw new BizException("WeChat phone authorization failed: empty phone number");
    }
    return phone;
  }

  private void bindMobile(UserAccount user, String mobile) {
    String value = trim(mobile);
    if (value.isEmpty()) return;
    UserAuth auth = authRepo.findByUser(user).orElseGet(() -> {
      UserAuth a = new UserAuth();
      a.setUser(user);
      a.setStatus(AuthStatus.UNVERIFIED);
      return a;
    });
    auth.setMobile(value);
    authRepo.save(auth);
  }

  private static boolean isValidMobile(String mobile) {
    return trim(mobile).matches("^1\\d{10}$");
  }

  private static String requireEmail(String email) {
    String value = EmailCodeService.normalizeEmail(email);
    if (value.isEmpty()) throw new BizException("email format invalid");
    return value;
  }

  private static String requirePassword(String password) {
    String value = trim(password);
    if (value.length() < 6 || value.length() > 32) {
      throw new BizException("password length must be 6-32");
    }
    return value;
  }

  private static String enc(String s) {
    return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
  }

  private static String trim(String s) {
    return s == null ? "" : s.trim();
  }

  private static boolean isInvalidWxConfig(String v) {
    String x = trim(v).toLowerCase();
    return x.isEmpty() || "undefined".equals(x) || "null".equals(x);
  }

  private static String safe(String s) {
    return s == null ? "" : s;
  }

  private static String maskIdNo(String encryptedOrRaw) {
    String s = trim(encryptedOrRaw);
    if (s.isEmpty()) return "";
    if (s.startsWith("ENC:")) return "已保存";
    if (s.length() <= 4) return "****";
    return s.substring(0, 2) + "********" + s.substring(s.length() - 2);
  }

  public record ProfileDto(String realName, String idNoMasked, String mobile, String payAccount) {}

  private record WxSessionResp(String openid, String sessionKey) {}

  private Map<String, Object> parseWxBody(String raw) {
    String body = trim(raw);
    if (body.isEmpty()) {
      return Collections.emptyMap();
    }
    try {
      return objectMapper.readValue(body, new TypeReference<>() {});
    } catch (Exception e) {
      throw new BizException("WeChat login request failed: invalid response body -> " + body);
    }
  }

  private static int parseErrCode(Object value) {
    if (value == null) return 0;
    if (value instanceof Number n) return n.intValue();
    try {
      return Integer.parseInt(String.valueOf(value).trim());
    } catch (Exception ignore) {
      return -1;
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asMap(Object value) {
    if (value instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    return Collections.emptyMap();
  }

  private static String normalizeGender(String gender) {
    String v = safe(gender).trim().toUpperCase(Locale.ROOT);
    if (v.isEmpty()) return "";
    if ("MALE".equals(v) || "FEMALE".equals(v) || "UNKNOWN".equals(v)) return v;
    throw new BizException("invalid gender");
  }

  private static String defaultNickname(String openid) {
    String id = safe(openid).trim();
    if (id.isEmpty()) return "微信用户";
    int start = Math.max(0, id.length() - 6);
    return "微信用户" + id.substring(start);
  }

  private static String defaultPhoneNickname(String mobile) {
    String s = trim(mobile);
    String seed = s.isEmpty() ? String.valueOf(System.nanoTime()) : s;
    String suffix = Integer.toString(Math.abs(seed.hashCode()), 36);
    if (suffix.length() > 7) suffix = suffix.substring(0, 7);
    return "叼瓜赖_" + suffix;
  }

  private static String defaultEmailNickname(String email) {
    String seed = trim(email);
    if (seed.isEmpty()) seed = String.valueOf(System.nanoTime());
    String suffix = Integer.toString(Math.abs(seed.hashCode()), 36);
    if (suffix.length() > 7) suffix = suffix.substring(0, 7);
    return "叼瓜赖_" + suffix;
  }

  private static String defaultAvatar(String seed, String avatar) {
    String v = safe(avatar).trim();
    if (!v.isEmpty()) return v;
    String s = java.net.URLEncoder.encode(safe(seed).isEmpty() ? String.valueOf(System.nanoTime()) : safe(seed), java.nio.charset.StandardCharsets.UTF_8);
    return "https://api.dicebear.com/8.x/adventurer/svg?seed=" + s;
  }
}
