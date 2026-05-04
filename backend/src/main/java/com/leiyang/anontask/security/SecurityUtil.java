package com.leiyang.anontask.security;

import com.leiyang.anontask.common.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {
  private SecurityUtil() {}

  public static AuthPrincipal principal() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal p)) {
      throw new BizException("Unauthorized");
    }
    return p;
  }

  public static long requireMpUserId() {
    AuthPrincipal p = principal();
    if (p.role() != AuthRole.MP_USER) {
      throw new BizException("Forbidden");
    }
    return p.id();
  }

  public static long requireAdminId() {
    AuthPrincipal p = principal();
    if (p.role() != AuthRole.ADMIN) {
      throw new BizException("Forbidden");
    }
    return p.id();
  }
}

