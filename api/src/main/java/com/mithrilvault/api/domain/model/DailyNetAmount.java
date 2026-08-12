package com.mithrilvault.api.domain.model;

import java.time.LocalDate;

public record DailyNetAmount(LocalDate date, Long netAmount) {}
