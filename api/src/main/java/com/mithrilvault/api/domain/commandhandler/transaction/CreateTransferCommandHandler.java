package com.mithrilvault.api.domain.commandhandler.transaction;

import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.model.Transaction;
import com.mithrilvault.api.domain.port.AccountRepository;
import com.mithrilvault.api.domain.port.TransactionReadRepository;
import com.mithrilvault.api.domain.port.TransactionRepository;
import com.mithrilvault.api.domain.service.TransactionValidationService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateTransferCommandHandler {
  private final AccountRepository accountRepository;
  private final TransactionRepository transactionRepository;
  private final TransactionReadRepository transactionReadRepository;
  private final TransactionValidationService validationService;

  private record TransferAccounts(Account source, Account target) {}

  public Flux<Transaction> handle(CreateTransactionCommand command, String ownerId) {

    String providedTransferPairId = command.transfer().transferPairId();

    String transferPairId =
        Optional.ofNullable(providedTransferPairId).orElse(UUID.randomUUID().toString());

    Flux<Transaction> existingTransferPair =
        providedTransferPairId != null
            ? transactionReadRepository.findByTransferPairId(ownerId, transferPairId)
            : Flux.empty();

    return validationService
        .validate(command)
        .thenMany(
            existingTransferPair.switchIfEmpty(
                findTransferAccounts(ownerId, command)
                    .map(accounts -> createTransferLeg(ownerId, command, accounts, transferPairId))
                    .flatMapMany(transactionRepository::saveAll)));
  }

  private Mono<Account> findOwnedAccount(String ownerId, String accountId, String accountType) {
    return accountRepository
        .findByIdAndOwnerId(accountId, ownerId)
        .switchIfEmpty(
            Mono.defer(
                () -> Mono.error(new NotFoundException(accountType + " account not found"))));
  }

  private Mono<TransferAccounts> findTransferAccounts(
      String ownerId, CreateTransactionCommand command) {
    return Mono.zip(
            findOwnedAccount(ownerId, command.accountId(), "Source"),
            findOwnedAccount(ownerId, command.transfer().destinationAccountId(), "Target"))
        .map(result -> new TransferAccounts(result.getT1(), result.getT2()));
  }

  private List<Transaction> createTransferLeg(
      String ownerId,
      CreateTransactionCommand command,
      TransferAccounts accounts,
      String transferPairId) {
    return Transaction.transferLeg(
        command, accounts.source(), accounts.target(), ownerId, transferPairId);
  }
}
