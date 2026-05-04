package com.leiyang.anontask.service;

import com.leiyang.anontask.common.BizException;
import jakarta.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import com.leiyang.anontask.config.AsyncMessagingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class EmailCodeService {
  private static final Logger log = LoggerFactory.getLogger(EmailCodeService.class);
  private static final String DEV_CODE = "123456";
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String CODE_KEY_PREFIX = "auth:email:code:";
  private static final String SENT_KEY_PREFIX = "auth:email:sent:";

  private final Map<String, CodeEntry> codes = new ConcurrentHashMap<>();
  private final Environment env;
  private final ObjectProvider<StringRedisTemplate> redisProvider;
  private final MessageQueueService queueService;

  public EmailCodeService(
      Environment env,
      ObjectProvider<StringRedisTemplate> redisProvider,
      MessageQueueService queueService
  ) {
    this.env = env;
    this.redisProvider = redisProvider;
    this.queueService = queueService;
  }

  public record EmailSendEvent(String email, String code) {}

  public void send(String email) {
    String target = normalizeEmail(email);
    if (target.isEmpty()) throw new BizException("email format invalid");
    Instant now = Instant.now();
    String code = String.format("%06d", RANDOM.nextInt(1_000_000));
    boolean redisSaved = false;
    if (!saveCodeToRedis(target, code)) {
      CodeEntry existing = codes.get(target);
      if (existing != null && existing.sentAt().plusSeconds(60).isAfter(now)) {
        throw new BizException("verification code sent too frequently");
      }
      codes.put(target, new CodeEntry(code, now.plus(1, ChronoUnit.MINUTES), now));
    } else {
      redisSaved = true;
    }
    boolean queued = queueService.publish(AsyncMessagingConfig.EMAIL_ROUTING_KEY, new EmailSendEvent(target, code));
    log.info("email_code_created email={} storage={} queued={}", maskEmail(target), redisSaved ? "redis" : "local", queued);
    if (!queued) {
      sendMailNow(target, code);
    }
  }

  public void verify(String email, String code) {
    String target = normalizeEmail(email);
    String value = trim(code);
    if (target.isEmpty()) throw new BizException("email format invalid");
    if (value.isEmpty()) throw new BizException("verification code is required");
    if (DEV_CODE.equals(value)) {
      log.info("email_code_verify_dev email={}", maskEmail(target));
      return;
    }
    String redisCode = getRedisCode(target);
    if (redisCode != null) {
      if (!redisCode.equals(value)) {
        log.info("email_code_verify_failed email={} reason=invalid redis=true", maskEmail(target));
        throw new BizException("verification code invalid");
      }
      log.info("email_code_verify_success email={} redis=true", maskEmail(target));
      return;
    }
    CodeEntry entry = codes.get(target);
    if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
      log.info("email_code_verify_failed email={} reason=expired redis=false", maskEmail(target));
      throw new BizException("verification code expired");
    }
    if (!entry.code().equals(value)) {
      log.info("email_code_verify_failed email={} reason=invalid redis=false", maskEmail(target));
      throw new BizException("verification code invalid");
    }
    log.info("email_code_verify_success email={} redis=false", maskEmail(target));
  }

  public void consume(String email, String code) {
    verify(email, code);
    if (!DEV_CODE.equals(trim(code))) {
      String target = normalizeEmail(email);
      deleteRedisCode(target);
      codes.remove(target);
    }
  }

  public void sendMailNow(String email, String code) {
    String host = trim(env.getProperty("email.hostName", ""));
    String from = trim(env.getProperty("email.email", ""));
    String senderName = trim(env.getProperty("email.emailName", "叼瓜赖圈"));
    String password = trim(env.getProperty("email.emailPassword", ""));
    if (host.isEmpty() || from.isEmpty() || password.isEmpty()) {
      log.warn("email_send_skipped email={} reason=smtpNotConfigured", maskEmail(email));
      return;
    }
    try {
      JavaMailSenderImpl sender = new JavaMailSenderImpl();
      sender.setHost(host);
      sender.setPort(env.getProperty("email.port", Integer.class, 465));
      sender.setUsername(from);
      sender.setPassword(password);
      sender.setDefaultEncoding("UTF-8");
      Properties props = sender.getJavaMailProperties();
      props.put("mail.smtp.auth", "true");
      props.put("mail.smtp.ssl.enable", String.valueOf(env.getProperty("email.ssl", Boolean.class, true)));
      props.put("mail.smtp.ssl.trust", env.getProperty("email.sslTrust", "*"));
      props.put("mail.smtp.starttls.enable", String.valueOf(env.getProperty("email.starttls", Boolean.class, false)));
      props.put("mail.smtp.starttls.required", String.valueOf(env.getProperty("email.starttls", Boolean.class, false)));
      props.put("mail.smtp.connectiontimeout", "8000");
      props.put("mail.smtp.timeout", "8000");

      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
      helper.setFrom(from, senderName.isEmpty() ? from : senderName);
      helper.setTo(email);
      helper.setSubject("叼瓜赖圈验证码");
      helper.setText("您的验证码为 " + code + "，1分钟内有效。若非本人操作，请忽略本邮件。", false);
      sender.send(message);
      log.info("email_send_success email={} host={}", maskEmail(email), host);
    } catch (Exception e) {
      log.warn("email_send_failed email={} host={} error={}", maskEmail(email), host, cleanError(e.getMessage()));
      throw new BizException("email send failed: " + cleanError(e.getMessage()));
    }
  }

  private boolean saveCodeToRedis(String email, String code) {
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis == null) return false;
      String sentKey = SENT_KEY_PREFIX + email;
      Boolean first = redis.opsForValue().setIfAbsent(sentKey, "1", 60, TimeUnit.SECONDS);
      if (!Boolean.TRUE.equals(first)) throw new BizException("verification code sent too frequently");
      redis.opsForValue().set(CODE_KEY_PREFIX + email, code, 60, TimeUnit.SECONDS);
      return true;
    } catch (BizException e) {
      throw e;
    } catch (Exception e) {
      return false;
    }
  }

  private String getRedisCode(String email) {
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis == null) return null;
      return redis.opsForValue().get(CODE_KEY_PREFIX + email);
    } catch (Exception e) {
      return null;
    }
  }

  private void deleteRedisCode(String email) {
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis != null) redis.delete(CODE_KEY_PREFIX + email);
    } catch (Exception ignored) {
      // fall back map removal still happens in caller
    }
  }

  private static String cleanError(String message) {
    String v = trim(message);
    if (v.isEmpty()) return "unknown error";
    if (v.length() > 180) v = v.substring(0, 180);
    return v;
  }

  public static String normalizeEmail(String email) {
    String v = trim(email).toLowerCase(Locale.ROOT);
    if (!v.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) return "";
    return v;
  }

  private static String trim(String s) {
    return s == null ? "" : s.trim();
  }

  private static String maskEmail(String email) {
    String v = trim(email);
    int at = v.indexOf('@');
    if (at <= 1) return "***" + (at >= 0 ? v.substring(at) : "");
    return v.charAt(0) + "***" + v.substring(at);
  }

  private record CodeEntry(String code, Instant expiresAt, Instant sentAt) {}
}
