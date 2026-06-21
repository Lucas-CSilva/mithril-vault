package com.mithrilvault.api.domain.port;

public interface TokenProvider {

  String generateAccessToken(String userId, String email);

  String generateRefreshToken(String userId);
}
