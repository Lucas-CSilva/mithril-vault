package com.mithrilvault.api.domain.model;

public record TransactionOrigin(Account account, Card card, Invoice invoice) {}
