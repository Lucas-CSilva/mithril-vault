package com.mithrilvault.api.domain.command.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginCommand(@Email @NotBlank String email, @NotBlank String password) {

  @Override
  public String toString() {
    return "LoginCommand[email=" + email + ", password=[PROTECTED]]";
  }
}
