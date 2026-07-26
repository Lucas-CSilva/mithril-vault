package com.mithrilvault.api.domain.service;

import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.exception.NotImplementedException;
import com.mithrilvault.api.domain.model.PaymentMethod;
import com.mithrilvault.api.domain.model.TransactionOrigin;
import com.mithrilvault.api.domain.port.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TransactionOriginResolver {

  private final AccountRepository accountRepository;

  public Mono<TransactionOrigin> resolve(CreateTransactionCommand command, String ownerId) {
    // TODO(005-cards): implement once Card/Invoice persistence exists — CardReadRepository and
    // InvoiceReadRepository are still stub adapters (spec 005 not built yet).
    if (command.paymentMethod() == PaymentMethod.CREDIT_CARD
        || command.paymentMethod() == PaymentMethod.DEBIT_CARD) {
      return Mono.error(
          new NotImplementedException(
              "Card-based transactions are not implemented yet (pending spec 005)"));
    }

    return accountRepository
        .findByIdAndOwnerId(command.accountId(), ownerId)
        .switchIfEmpty(Mono.error(new NotFoundException("Account not found")))
        .map(account -> new TransactionOrigin(account, null, null));
  }
}
