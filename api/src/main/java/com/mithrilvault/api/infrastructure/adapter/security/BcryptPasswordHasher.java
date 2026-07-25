package com.mithrilvault.api.infrastructure.adapter.security;

import com.mithrilvault.api.domain.port.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasher {

  private final PasswordEncoder encoder = new BCryptPasswordEncoder();

  @Override
  public String hash(String raw) {
    return encoder.encode(raw);
  }

  @Override
  public boolean matches(String raw, String hash) {
    return encoder.matches(raw, hash);
  }
}
