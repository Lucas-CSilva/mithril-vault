package com.mithrilvault.api.domain.commandhandler.user;

import static com.mithrilvault.api.domain.model.UserStatus.ACTIVE;

import com.mithrilvault.api.domain.command.user.RegisterUserCommand;
import com.mithrilvault.api.domain.exception.ConflictException;
import com.mithrilvault.api.domain.model.User;
import com.mithrilvault.api.domain.port.PasswordHasher;
import com.mithrilvault.api.domain.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RegisterUserCommandHandler {

  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;

  public Mono<User> handle(RegisterUserCommand command) {
    return userRepository
        .existsByEmail(command.email().toLowerCase())
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(new ConflictException("Email already registered"));
              }

              User user =
                  User.builder()
                      .email(command.email().toLowerCase())
                      .passwordHash(passwordHasher.hash(command.rawPassword()))
                      .displayName(command.displayName())
                      .status(ACTIVE)
                      .build();

              return userRepository.save(user);
            });
  }
}
