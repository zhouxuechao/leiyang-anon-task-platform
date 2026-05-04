package com.leiyang.anontask.security;

import com.leiyang.anontask.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
  private final AppProperties props;
  private final SecretKey key;

  public TokenService(AppProperties props) {
    this.props = props;
    this.key = Keys.hmacShaKeyFor(props.jwt().secret().getBytes(StandardCharsets.UTF_8));
  }

  public String issueToken(AuthPrincipal principal) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(props.jwt().expireMinutes() * 60);
    return Jwts.builder()
        .issuer(props.jwt().issuer())
        .subject(Long.toString(principal.id()))
        .claim("role", principal.role().name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public AuthPrincipal parse(String token) {
    Claims claims = Jwts.parser()
        .verifyWith(key)
        .requireIssuer(props.jwt().issuer())
        .build()
        .parseSignedClaims(token)
        .getPayload();
    long id = Long.parseLong(claims.getSubject());
    String role = claims.get("role", String.class);
    return new AuthPrincipal(id, AuthRole.valueOf(role));
  }
}

