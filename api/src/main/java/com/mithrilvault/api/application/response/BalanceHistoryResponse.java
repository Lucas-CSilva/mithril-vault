package com.mithrilvault.api.application.response;

import com.mithrilvault.api.domain.model.BalancePoint;
import java.util.List;

public record BalanceHistoryResponse(String accountId, List<BalancePoint> points) {}
