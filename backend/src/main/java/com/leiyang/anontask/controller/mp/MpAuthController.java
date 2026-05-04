package com.leiyang.anontask.controller.mp;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.dto.mp.MpLoginRequest;
import com.leiyang.anontask.dto.mp.MpLoginResponse;
import com.leiyang.anontask.dto.mp.MpUserDto;
import com.leiyang.anontask.dto.mp.PhoneLoginRequest;
import com.leiyang.anontask.dto.mp.WxLoginRequest;
import com.leiyang.anontask.security.SecurityUtil;
import com.leiyang.anontask.service.MpAuthService;
import com.leiyang.anontask.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/mp/auth")
public class MpAuthController {
  private final MpAuthService authService;
  private final UserService userService;

  public MpAuthController(MpAuthService authService, UserService userService) {
    this.authService = authService;
    this.userService = userService;
  }

  @PostMapping("/mock-login")
  public ApiResult<MpLoginResponse> mockLogin(@Valid @RequestBody MpLoginRequest req) {
    return ApiResult.ok(authService.mockLogin(req));
  }

  @PostMapping("/wx-login")
  public ApiResult<MpLoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest req) {
    return ApiResult.ok(authService.wxLogin(req));
  }

  @PostMapping("/phone/send-code")
  public ApiResult<Void> sendPhoneCode(@Valid @RequestBody PhoneCodeRequest req) {
    authService.sendPhoneCode(req.mobile());
    return ApiResult.ok(null);
  }

  @PostMapping("/phone-login")
  public ApiResult<MpLoginResponse> phoneLogin(@Valid @RequestBody PhoneLoginRequest req) {
    return ApiResult.ok(authService.phoneLogin(req));
  }

  @PostMapping("/email/status")
  public ApiResult<EmailStatusResponse> emailStatus(@Valid @RequestBody EmailCodeRequest req) {
    return ApiResult.ok(new EmailStatusResponse(authService.isEmailRegistered(req.email())));
  }

  @PostMapping("/email/send-code")
  public ApiResult<Void> sendEmailCode(@Valid @RequestBody EmailCodeRequest req) {
    authService.sendEmailCode(req.email());
    return ApiResult.ok(null);
  }

  @PostMapping("/email/verify-code")
  public ApiResult<Void> verifyEmailCode(@Valid @RequestBody EmailCodeLoginRequest req) {
    authService.verifyEmailCode(req.email(), req.code());
    return ApiResult.ok(null);
  }

  @PostMapping("/email-register")
  public ApiResult<MpLoginResponse> emailRegister(@Valid @RequestBody EmailRegisterRequest req) {
    return ApiResult.ok(authService.emailRegister(req.email(), req.code(), req.password()));
  }

  @PostMapping("/email-code-login")
  public ApiResult<MpLoginResponse> emailCodeLogin(@Valid @RequestBody EmailCodeLoginRequest req) {
    return ApiResult.ok(authService.emailCodeLogin(req.email(), req.code()));
  }

  @PostMapping("/email-password-login")
  public ApiResult<MpLoginResponse> emailPasswordLogin(@Valid @RequestBody EmailPasswordLoginRequest req) {
    return ApiResult.ok(authService.emailPasswordLogin(req.email(), req.password()));
  }

  @GetMapping("/me")
  public ApiResult<?> me() {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    return ApiResult.ok(authService.toDto(user));
  }

  public record UpdateMeRequest(
      @Size(max = 64, message = "nickname too long") String nickname,
      @Size(max = 512, message = "avatar too long") String avatar,
      @Size(max = 16, message = "gender too long") String gender,
      @Size(max = 255, message = "signature too long") String signature
  ) {}

  public record PhoneCodeRequest(
      @jakarta.validation.constraints.NotBlank(message = "mobile is required")
      @jakarta.validation.constraints.Pattern(regexp = "^1\\d{10}$", message = "mobile format invalid")
      String mobile
  ) {}

  public record EmailCodeRequest(
      @jakarta.validation.constraints.NotBlank(message = "email is required")
      @jakarta.validation.constraints.Email(message = "email format invalid")
      String email
  ) {}

  public record EmailStatusResponse(boolean registered) {}

  public record EmailRegisterRequest(
      @jakarta.validation.constraints.NotBlank(message = "email is required")
      @jakarta.validation.constraints.Email(message = "email format invalid")
      String email,
      @jakarta.validation.constraints.NotBlank(message = "code is required")
      String code,
      @jakarta.validation.constraints.NotBlank(message = "password is required")
      @Size(min = 6, max = 32, message = "password length must be 6-32")
      String password
  ) {}

  public record EmailCodeLoginRequest(
      @jakarta.validation.constraints.NotBlank(message = "email is required")
      @jakarta.validation.constraints.Email(message = "email format invalid")
      String email,
      @jakarta.validation.constraints.NotBlank(message = "code is required")
      String code
  ) {}

  public record EmailPasswordLoginRequest(
      @jakarta.validation.constraints.NotBlank(message = "email is required")
      @jakarta.validation.constraints.Email(message = "email format invalid")
      String email,
      @jakarta.validation.constraints.NotBlank(message = "password is required")
      String password
  ) {}

  @PostMapping("/me")
  public ApiResult<MpUserDto> updateMe(@Valid @RequestBody UpdateMeRequest req) {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    return ApiResult.ok(authService.updateMe(user, req.nickname(), req.avatar(), req.gender(), req.signature()));
  }
}
