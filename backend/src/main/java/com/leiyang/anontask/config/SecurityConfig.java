package com.leiyang.anontask.config;

import com.leiyang.anontask.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter)
      throws Exception {
    http.cors(cors -> {});
    http.csrf(csrf -> csrf.disable());
    http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.httpBasic(basic -> basic.disable());
    http.formLogin(form -> form.disable());

    http.authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.GET, "/api/mini/music/song/*/comments").permitAll()
        .requestMatchers(
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error",
            "/uploads/**",
            "/api/public/suno/callback",
            "/api/public/wxpay/recharge-notify",
            "/api/mp/public/**",
            "/api/mp/public/plaza/**",
            "/api/mp/auth/mock-login",
            "/api/mp/auth/wx-login",
            "/api/mp/auth/phone-login",
            "/api/mp/auth/phone/send-code",
            "/api/mp/auth/email/status",
            "/api/mp/auth/email/send-code",
            "/api/mp/auth/email/verify-code",
            "/api/mp/auth/email-register",
            "/api/mp/auth/email-code-login",
            "/api/mp/auth/email-password-login",
            "/api/mini/music/packages",
            "/api/mini/music/hall",
            "/api/mini/music/song/*",
            "/api/admin/auth/login"
        ).permitAll()
        .requestMatchers(
            "/api/mp/home/**",
            "/api/mp/tasks",
            "/api/mp/tasks/*"
        ).permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .requestMatchers("/api/mini/**").hasRole("MP_USER")
        .requestMatchers("/api/mp/**").hasRole("MP_USER")
        .requestMatchers("/api/common/**").hasAnyRole("MP_USER", "ADMIN")
        .anyRequest().denyAll()
    );

    http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
