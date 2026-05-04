package com.leiyang.anontask.service;

import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.domain.AdminUser;
import com.leiyang.anontask.dto.admin.AdminLoginRequest;
import com.leiyang.anontask.dto.admin.AdminLoginResponse;
import com.leiyang.anontask.repo.AdminUserRepository;
import com.leiyang.anontask.security.AuthPrincipal;
import com.leiyang.anontask.security.AuthRole;
import com.leiyang.anontask.security.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {
  private final AdminUserRepository repo;
  private final PasswordEncoder encoder;
  private final TokenService tokenService;

  public AdminAuthService(AdminUserRepository repo, PasswordEncoder encoder, TokenService tokenService) {
    this.repo = repo;
    this.encoder = encoder;
    this.tokenService = tokenService;
  }

  public AdminLoginResponse login(AdminLoginRequest req) {
    AdminUser u = repo.findByUsername(req.username()).orElseThrow(() -> new BizException("Invalid credentials"));
    if (!"ACTIVE".equalsIgnoreCase(u.getStatus())) {
      throw new BizException("Admin disabled");
    }
    if (!encoder.matches(req.password(), u.getPasswordHash())) {
      throw new BizException("Invalid credentials");
    }
    String token = tokenService.issueToken(new AuthPrincipal(u.getId(), AuthRole.ADMIN));
    return new AdminLoginResponse(token);
  }
}

