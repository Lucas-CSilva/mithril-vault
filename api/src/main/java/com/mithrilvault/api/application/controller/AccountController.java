package com.mithrilvault.api.application.controller;

import com.mithrilvault.api.application.mapper.AccountResponseMapper;
import com.mithrilvault.api.application.response.AccountResponse;
import com.mithrilvault.api.application.response.BalanceHistoryResponse;
import com.mithrilvault.api.application.security.CurrentOwnerId;
import com.mithrilvault.api.domain.command.account.CreateAccountCommand;
import com.mithrilvault.api.domain.command.account.ReconcileAccountCommand;
import com.mithrilvault.api.domain.command.account.UpdateAccountCommand;
import com.mithrilvault.api.domain.commandhandler.account.CreateAccountCommandHandler;
import com.mithrilvault.api.domain.commandhandler.account.DeactivateAccountCommandHandler;
import com.mithrilvault.api.domain.commandhandler.account.ReactivateAccountCommandHandler;
import com.mithrilvault.api.domain.commandhandler.account.ReconcileAccountCommandHandler;
import com.mithrilvault.api.domain.commandhandler.account.UpdateAccountCommandHandler;
import com.mithrilvault.api.domain.queryhandler.account.GetAccountBalanceHistoryQueryHandler;
import com.mithrilvault.api.domain.queryhandler.account.GetAccountQueryHandler;
import com.mithrilvault.api.domain.queryhandler.account.ListAccountQueryHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts")
public class AccountController {

  private final AccountResponseMapper accountResponseMapper;
  private final CreateAccountCommandHandler createCommandHandler;
  private final UpdateAccountCommandHandler updateCommandHandler;
  private final DeactivateAccountCommandHandler deactivateCommandHandler;
  private final ReactivateAccountCommandHandler reactivateCommandHandler;
  private final ReconcileAccountCommandHandler reconcileCommandHandler;
  private final GetAccountQueryHandler getQueryHandler;
  private final ListAccountQueryHandler listQueryHandler;
  private final GetAccountBalanceHistoryQueryHandler balanceHistoryQueryHandler;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<AccountResponse> create(
      @RequestBody @Valid CreateAccountCommand command, @CurrentOwnerId String ownerId) {
    return createCommandHandler.handle(ownerId, command).map(accountResponseMapper::toResponse);
  }

  @PatchMapping(path = "/{id}")
  public Mono<AccountResponse> update(
      @PathVariable String id,
      @RequestBody @Valid UpdateAccountCommand command,
      @CurrentOwnerId String ownerId) {
    return updateCommandHandler.handle(id, ownerId, command).map(accountResponseMapper::toResponse);
  }

  @DeleteMapping(path = "/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> deactivate(@PathVariable String id, @CurrentOwnerId String ownerId) {
    return deactivateCommandHandler.handle(id, ownerId);
  }

  @PostMapping(path = "/{id}/reactivate")
  public Mono<AccountResponse> reactivate(@PathVariable String id, @CurrentOwnerId String ownerId) {
    return reactivateCommandHandler.handle(id, ownerId).map(accountResponseMapper::toResponse);
  }

  @GetMapping(path = "/{id}")
  public Mono<AccountResponse> get(@PathVariable String id, @CurrentOwnerId String ownerId) {
    return getQueryHandler.handle(id, ownerId).map(accountResponseMapper::toResponse);
  }

  @GetMapping
  public Flux<AccountResponse> list(
      @RequestParam(defaultValue = "false") boolean includeInactive,
      @CurrentOwnerId String ownerId) {
    return listQueryHandler.handle(ownerId, includeInactive).map(accountResponseMapper::toResponse);
  }

  @PostMapping(path = "/{id}/reconcile")
  public Mono<AccountResponse> reconcile(
      @PathVariable String id,
      @RequestBody @Valid ReconcileAccountCommand command,
      @CurrentOwnerId String ownerId) {
    return reconcileCommandHandler
        .handle(id, ownerId, command)
        .map(accountResponseMapper::toResponse);
  }

  @GetMapping(path = "/{id}/balance-history")
  public Mono<BalanceHistoryResponse> balanceHistory(
      @PathVariable String id, @CurrentOwnerId String ownerId) {
    return balanceHistoryQueryHandler
        .handle(id, ownerId)
        .collectList()
        .map(points -> new BalanceHistoryResponse(id, points));
  }
}
