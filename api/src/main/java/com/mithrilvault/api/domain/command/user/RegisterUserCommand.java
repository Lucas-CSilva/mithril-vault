package com.mithrilvault.api.domain.command.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record RegisterUserCommand(
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password,
    String displayName) {

  @Override
  public String toString() {
    return "RegisterUserCommand[email="
        + email
        + ", password=[PROTECTED], displayName="
        + displayName
        + "]";
  }
}
