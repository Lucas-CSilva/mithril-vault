package com.mithrilvault.api.domain.port;

public interface PasswordHasher {

  String hash(String raw);

  boolean matches(String raw, String hash);
}
