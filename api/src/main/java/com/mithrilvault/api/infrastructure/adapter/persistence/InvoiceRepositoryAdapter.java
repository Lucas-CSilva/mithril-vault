package com.mithrilvault.api.infrastructure.adapter.persistence;

import com.mithrilvault.api.domain.model.Invoice;
import com.mithrilvault.api.domain.port.InvoiceReadRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class InvoiceRepositoryAdapter implements InvoiceReadRepository {

  @Override
  public Mono<Invoice> findOpenInvoice(String creditCardId, String ownerId, LocalDate date) {
    // TODO(005-cards): implement once Invoice persistence exists.
    return Mono.error(
        new UnsupportedOperationException("Invoice persistence is not implemented yet"));
  }
}
