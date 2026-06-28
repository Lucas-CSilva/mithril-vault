package com.mithrilvault.api.domain.result;

import com.mithrilvault.api.domain.model.User;

public record IssuedTokens(String accessToken, String rawRefreshToken, User user) {}
