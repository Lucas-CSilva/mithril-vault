package com.mithrilvault.api.infrastructure.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtProvider {

  private final AppProperties appProperties;

  public String generateAccessToken(String userId, String email) {
    Instant now = Instant.now();
    Instant expiry = now.plus(Duration.ofSeconds(appProperties.jwt().accessTokenTtlSeconds()));
    return buildToken(userId, email, now, expiry);
  }

  public String generateRefreshToken(String userId) {
    Instant now = Instant.now();
    Instant expiry = now.plus(Duration.ofSeconds(appProperties.jwt().refreshTokenTtlSeconds()));
    return buildToken(userId, null, now, expiry);
  }

  private String buildToken(String subject, String email, Instant issuedAt, Instant expiry) {
    try {
      byte[] keyBytes = appProperties.jwt().secretKey().getBytes();
      SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
      MACSigner signer = new MACSigner(secretKey);

      JWTClaimsSet.Builder claims =
          new JWTClaimsSet.Builder()
              .jwtID(UUID.randomUUID().toString())
              .subject(subject)
              .issueTime(Date.from(issuedAt))
              .expirationTime(Date.from(expiry));

      if (email != null) {
        claims.claim("email", email);
      }

      SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
      jwt.sign(signer);
      return jwt.serialize();
    } catch (Exception e) {
      throw new IllegalStateException("JWT signing failed", e);
    }
  }
}
