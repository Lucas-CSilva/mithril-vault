package com.mithrilvault.api.domain.command.auth;

import com.mithrilvault.api.domain.model.User;

public record IssueTokensCommand(User user) {}
