package com.mithrilvault.api.application.controller;

import com.mithrilvault.api.application.mapper.TransactionResponseMapper;
import com.mithrilvault.api.application.response.TransactionResponse;
import com.mithrilvault.api.application.security.CurrentOwnerId;
import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import com.mithrilvault.api.domain.commandhandler.transaction.CreateInstallmentCommandHandler;
import com.mithrilvault.api.domain.commandhandler.transaction.CreateRecurringTransactionCommandHandler;
import com.mithrilvault.api.domain.commandhandler.transaction.CreateTransactionCommandHandler;
import com.mithrilvault.api.domain.commandhandler.transaction.CreateTransferCommandHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transactions")
public class TransactionController {

  private final CreateTransactionCommandHandler createCommandHandler;
  private final CreateRecurringTransactionCommandHandler createRecurringCommandHandler;
  private final CreateInstallmentCommandHandler createInstallmentCommandHandler;
  private final CreateTransferCommandHandler createTransferCommandHandler;
  private final TransactionResponseMapper mapper;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Flux<TransactionResponse> create(
      @RequestBody @NotNull @Valid CreateTransactionCommand command,
      @CurrentOwnerId String ownerId) {

    var transaction =
        switch (command.mode()) {
          case SINGLE -> createCommandHandler.handle(command, ownerId);
          case RECURRING -> createRecurringCommandHandler.handle(command, ownerId);
          case INSTALLMENT -> createInstallmentCommandHandler.handle(command, ownerId);
          case TRANSFER -> createTransferCommandHandler.handle(command, ownerId);
        };

    return transaction.map(mapper::toResponse);
  }
}
