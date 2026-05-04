package com.leiyang.anontask.controller.admin;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.dto.admin.AdminLoginRequest;
import com.leiyang.anontask.dto.admin.AdminLoginResponse;
import com.leiyang.anontask.service.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
  private final AdminAuthService authService;

  public AdminAuthController(AdminAuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ApiResult<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest req) {
    return ApiResult.ok(authService.login(req));
  }
}

