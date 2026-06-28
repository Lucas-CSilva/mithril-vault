package com.mithrilvault.api.domain.commandhandler.auth;

import com.mithrilvault.api.domain.command.auth.LoginCommand;
import com.mithrilvault.api.domain.exception.UnauthorizedException;
import com.mithrilvault.api.domain.model.User;
import com.mithrilvault.api.domain.model.UserStatus;
import com.mithrilvault.api.domain.port.PasswordHasher;
import com.mithrilvault.api.domain.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class LoginCommandHandler {

  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;

  public Mono<User> handle(LoginCommand command) {
    return userRepository
        .findByEmail(command.email())
        .switchIfEmpty(Mono.error(new UnauthorizedException("Invalid credentials")))
        .flatMap(
            user -> {
              if (user.status() != UserStatus.ACTIVE) {
                return Mono.error(new UnauthorizedException("Account disabled"));
              }
              if (!passwordHasher.matches(command.password(), user.passwordHash())) {
                return Mono.error(new UnauthorizedException("Invalid credentials"));
              }
              return Mono.just(user);
            });
  }
}
