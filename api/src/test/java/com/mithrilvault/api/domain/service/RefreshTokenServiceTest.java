package com.mithrilvault.api.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.config.AppProperties;
import com.mithrilvault.api.domain.model.RefreshToken;
import com.mithrilvault.api.domain.port.RefreshTokenRepository;
import com.mithrilvault.api.domain.port.TokenProvider;
import com.mithrilvault.api.fixture.model.RefreshTokens;
import com.mithrilvault.api.fixture.model.Users;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock private AppProperties appProperties;
  @Mock private TokenProvider tokenProvider;
  @Mock private RefreshTokenRepository refreshTokenRepository;

  private RefreshTokenService service;

  @BeforeEach
  void setUp() {
    service = new RefreshTokenService(appProperties, tokenProvider, refreshTokenRepository);
    when(appProperties.jwt()).thenReturn(new AppProperties.Jwt("secret", 300L, 86_400L));
  }

  @Test
  void issueGeneratesHashesAndPersistsToken() {
    var user = Users.active();
    when(tokenProvider.generateRefreshToken(user.id())).thenReturn("raw-token");
    when(refreshTokenRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(service.issue(user))
        .assertNext(rawToken -> assertThat(rawToken).isEqualTo("raw-token"))
        .verifyComplete();

    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(captor.capture());
    RefreshToken saved = captor.getValue();

    assertThat(saved.userId()).isEqualTo(user.id());
    assertThat(saved.tokenHash()).isEqualTo(service.hash("raw-token"));
    assertThat(saved.expiresAt()).isAfter(Instant.now());
    assertThat(saved.id()).isNull();
    assertThat(saved.revokedAt()).isNull();
  }

  @Test
  void rotateIssuesNewTokenAndRevokesOldWithNewTokenHash() {
    var user = Users.active();
    var oldToken = RefreshTokens.active(user.id());
    when(tokenProvider.generateRefreshToken(user.id())).thenReturn("new-raw-token");
    when(refreshTokenRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(service.rotate(oldToken, user))
        .assertNext(rawToken -> assertThat(rawToken).isEqualTo("new-raw-token"))
        .verifyComplete();

    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository, times(2)).save(captor.capture());
    List<RefreshToken> saved = captor.getAllValues();

    RefreshToken newToken =
        saved.stream().filter(t -> t.revokedAt() == null).findFirst().orElseThrow();
    assertThat(newToken.userId()).isEqualTo(user.id());
    assertThat(newToken.tokenHash()).isEqualTo(service.hash("new-raw-token"));

    RefreshToken revokedOld =
        saved.stream().filter(t -> t.revokedAt() != null).findFirst().orElseThrow();
    assertThat(revokedOld.id()).isEqualTo(oldToken.id());
    assertThat(revokedOld.replacedBy()).isEqualTo(service.hash("new-raw-token"));
  }
}
