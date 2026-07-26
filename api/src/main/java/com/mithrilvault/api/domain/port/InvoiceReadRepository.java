package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.Invoice;
import java.time.LocalDate;
import reactor.core.publisher.Mono;

public interface InvoiceReadRepository {

  Mono<Invoice> findOpenInvoice(String creditCardId, String ownerId, LocalDate date);
}
