package com.mithrilvault.api.domain.model;

import com.mithrilvault.api.domain.command.account.CreateAccountCommand;
import com.mithrilvault.api.domain.command.account.UpdateAccountCommand;
import java.time.Instant;
import java.util.Optional;
import lombok.Builder;

@Builder(toBuilder = true)
public record Account(
    String id,
    String ownerId,
    String name,
    AccountType type,
    String institution,
    Long initialBalance,
    Long currentBalance,
    String color,
    Boolean isActive,
    Instant createdAt,
    Long version) {

  public static Account create(String ownerId, CreateAccountCommand command) {
    return Account.builder()
        .ownerId(ownerId)
        .name(command.name())
        .type(command.type())
        .institution(command.institution())
        .initialBalance(command.initialBalance())
        .currentBalance(command.initialBalance())
        .color(command.color())
        .isActive(Boolean.TRUE)
        .build();
  }

  public Account update(UpdateAccountCommand command) {
    var builder = this.toBuilder();

    Optional.ofNullable(command.name()).ifPresent(builder::name);
    Optional.ofNullable(command.type()).ifPresent(builder::type);
    Optional.ofNullable(command.institution()).ifPresent(builder::institution);
    Optional.ofNullable(command.color()).ifPresent(builder::color);

    return builder.build();
  }

  public Account deactivate() {
    return this.toBuilder().isActive(Boolean.FALSE).build();
  }

  public Account reactivate() {
    return this.toBuilder().isActive(Boolean.TRUE).build();
  }

  public Account reconcileBalances(Long newInitialBalance, Long newCurrentBalance) {
    return this.toBuilder()
        .initialBalance(newInitialBalance)
        .currentBalance(newCurrentBalance)
        .build();
  }
}
