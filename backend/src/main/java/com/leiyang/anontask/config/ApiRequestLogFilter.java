package com.leiyang.anontask.config;

import com.leiyang.anontask.security.AuthPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class ApiRequestLogFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(ApiRequestLogFilter.class);
  private final long slowRequestMs;

  public ApiRequestLogFilter(@Value("${app.api.slow-request-ms:300}") long slowRequestMs) {
    this.slowRequestMs = Math.max(50, slowRequestMs);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    long started = System.currentTimeMillis();
    String requestId = request.getHeader("X-Request-Id");
    Exception error = null;
    try {
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      error = e;
      throw e;
    } finally {
      long costMs = System.currentTimeMillis() - started;
      int status = response.getStatus();
      Long actorId = currentActorId();
      String method = request.getMethod();
      String uri = request.getRequestURI();
      String query = request.getQueryString();
      String path = query == null || query.isBlank() ? uri : uri + "?" + query;
      String ip = clientIp(request);
      if (error != null || status >= 500) {
        log.error(
            "api_request_failed method={} path={} status={} costMs={} actorId={} ip={} requestId={} error={}",
            method, path, status, costMs, actorId, ip, safe(requestId), error == null ? "" : error.toString(), error
        );
      } else if (status >= 400 || costMs >= slowRequestMs) {
        log.warn(
            "api_request_warn method={} path={} status={} costMs={} actorId={} ip={} requestId={}",
            method, path, status, costMs, actorId, ip, safe(requestId)
        );
      } else {
        log.debug(
            "api_request method={} path={} status={} costMs={} actorId={} ip={} requestId={}",
            method, path, status, costMs, actorId, ip, safe(requestId)
        );
      }
    }
  }

  private static Long currentActorId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof AuthPrincipal principal) return principal.id();
    return null;
  }

  private static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) return realIp.trim();
    return request.getRemoteAddr();
  }

  private static String safe(String value) {
    return value == null ? "" : value;
  }
}
