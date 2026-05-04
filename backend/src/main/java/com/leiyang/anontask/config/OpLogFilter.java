package com.leiyang.anontask.config;

import com.leiyang.anontask.security.AuthPrincipal;
import com.leiyang.anontask.security.AuthRole;
import com.leiyang.anontask.service.OpLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class OpLogFilter extends OncePerRequestFilter {
  private final OpLogService opLogService;

  public OpLogFilter(OpLogService opLogService) {
    this.opLogService = opLogService;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String p = request.getRequestURI();
    if (!p.startsWith("/api/admin/")) return true;
    return p.startsWith("/api/admin/auth/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    boolean ok = false;
    String err = null;
    try {
      filterChain.doFilter(request, response);
      ok = response.getStatus() < 400;
    } catch (Exception e) {
      err = e.getMessage();
      throw e;
    } finally {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      long actorId = 0;
      String actorType = "ANON";
      if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p) {
        actorId = p.id();
        actorType = p.role() == AuthRole.ADMIN ? "ADMIN" : "MP_USER";
      }
      String ip = request.getRemoteAddr();
      String ua = request.getHeader("User-Agent");
      opLogService.log(actorType, actorId, request.getMethod(), request.getRequestURI(), ip, ua, ok, err);
    }
  }
}

