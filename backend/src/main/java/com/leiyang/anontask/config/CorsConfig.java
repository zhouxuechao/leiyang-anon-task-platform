package com.leiyang.anontask.config;

import java.util.List;
import java.util.stream.Stream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {
  private final AppProperties props;

  public CorsConfig(AppProperties props) {
    this.props = props;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(allowedOrigins());
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    cfg.setAllowCredentials(false);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }

  private List<String> allowedOrigins() {
    String configured = props.cors() == null ? "" : safe(props.cors().allowedOrigins());
    if (!configured.isEmpty()) {
      return Stream.of(configured.split(","))
          .map(String::trim)
          .filter(v -> !v.isEmpty())
          .toList();
    }
    return List.of(
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:5174",
        "http://127.0.0.1:5174",
        "http://localhost:5175",
        "http://127.0.0.1:5175"
    );
  }

  private static String safe(String v) {
    return v == null ? "" : v.trim();
  }
}
