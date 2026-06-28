package com.mithrilvault.api.application.controller;

import com.mithrilvault.api.application.response.AuthResponse;
import com.mithrilvault.api.domain.command.auth.IssueTokensCommand;
import com.mithrilvault.api.domain.command.auth.LoginCommand;
import com.mithrilvault.api.domain.command.auth.LogoutCommand;
import com.mithrilvault.api.domain.command.auth.RefreshCommand;
import com.mithrilvault.api.domain.command.user.RegisterUserCommand;
import com.mithrilvault.api.domain.commandhandler.auth.IssueTokensCommandHandler;
import com.mithrilvault.api.domain.commandhandler.auth.LoginCommandHandler;
import com.mithrilvault.api.domain.commandhandler.auth.LogoutCommandHandler;
import com.mithrilvault.api.domain.commandhandler.auth.RefreshCommandHandler;
import com.mithrilvault.api.domain.commandhandler.user.RegisterUserCommandHandler;
import com.mithrilvault.api.domain.config.AppProperties;
import com.mithrilvault.api.domain.model.User;
import com.mithrilvault.api.domain.result.IssuedTokens;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AuthController {

  private static final String ACCESS_TOKEN_COOKIE = "accessToken";
  private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

  private final RegisterUserCommandHandler registerUserCommandHandler;
  private final LoginCommandHandler loginCommandHandler;
  private final IssueTokensCommandHandler issueTokensCommandHandler;
  private final RefreshCommandHandler refreshCommandHandler;
  private final LogoutCommandHandler logoutCommandHandler;
  private final AppProperties appProperties;

  @PostMapping("/register")
  public Mono<ResponseEntity<AuthResponse>> register(
      @RequestBody @Valid RegisterUserCommand command, ServerWebExchange exchange) {

    return registerUserCommandHandler
        .handle(command)
        .flatMap(user -> issueTokensAndRespond(user, exchange, HttpStatus.CREATED));
  }

  @PostMapping("/login")
  public Mono<ResponseEntity<AuthResponse>> login(
      @RequestBody @Valid LoginCommand command, ServerWebExchange exchange) {

    return loginCommandHandler
        .handle(command)
        .flatMap(user -> issueTokensAndRespond(user, exchange, HttpStatus.OK));
  }

  @PostMapping("/refresh")
  public Mono<ResponseEntity<AuthResponse>> refresh(ServerWebExchange exchange) {
    var refreshCookie = exchange.getRequest().getCookies().getFirst(REFRESH_TOKEN_COOKIE);

    if (refreshCookie == null) {
      return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    return refreshCommandHandler
        .handle(new RefreshCommand(refreshCookie.getValue()))
        .map(tokens -> buildAuthResponse(tokens, exchange.getResponse(), HttpStatus.OK));
  }

  @PostMapping("/logout")
  public Mono<ResponseEntity<Void>> logout(ServerWebExchange exchange) {

    var refreshCookie = exchange.getRequest().getCookies().getFirst(REFRESH_TOKEN_COOKIE);

    clearAuthCookies(exchange.getResponse());

    if (refreshCookie == null) {
      return Mono.just(ResponseEntity.noContent().build());
    }

    return logoutCommandHandler
        .handle(new LogoutCommand(refreshCookie.getValue()))
        .thenReturn(ResponseEntity.noContent().build());
  }

  private Mono<ResponseEntity<AuthResponse>> issueTokensAndRespond(
      User user, ServerWebExchange exchange, HttpStatus status) {

    return issueTokensCommandHandler
        .handle(new IssueTokensCommand(user))
        .map(tokens -> buildAuthResponse(tokens, exchange.getResponse(), status));
  }

  private ResponseEntity<AuthResponse> buildAuthResponse(
      IssuedTokens tokens, ServerHttpResponse response, HttpStatus status) {

    setAccessTokenCookie(response, tokens.accessToken());
    setRefreshTokenCookie(response, tokens.rawRefreshToken());
    return ResponseEntity.status(status)
        .body(new AuthResponse(tokens.user().email(), tokens.user().displayName()));
  }

  private void setAccessTokenCookie(ServerHttpResponse response, String value) {
    response.addCookie(
        ResponseCookie.from(ACCESS_TOKEN_COOKIE, value)
            .httpOnly(true)
            .path("/")
            .maxAge(appProperties.jwt().accessTokenTtlSeconds())
            .sameSite("Lax")
            .build());
  }

  private void setRefreshTokenCookie(ServerHttpResponse response, String value) {
    response.addCookie(
        ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
            .httpOnly(true)
            .path("/")
            .maxAge(appProperties.jwt().refreshTokenTtlSeconds())
            .sameSite("Lax")
            .build());
  }

  private void clearAuthCookies(ServerHttpResponse response) {
    response.addCookie(
        ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
            .httpOnly(true)
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .build());
    response.addCookie(
        ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
            .httpOnly(true)
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .build());
  }
}
