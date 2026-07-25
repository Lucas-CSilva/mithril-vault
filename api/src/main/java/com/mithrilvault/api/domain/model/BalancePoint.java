package com.mithrilvault.api.domain.model;

import java.time.LocalDate;

public record BalancePoint(LocalDate date, Long balance) {}
