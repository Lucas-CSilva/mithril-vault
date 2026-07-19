package com.mithrilvault.api.infrastructure.config;

import com.mithrilvault.api.domain.config.AppProperties;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpCookie;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(
      ServerHttpSecurity http,
      AppProperties appProperties,
      CorsConfigurationSource corsConfigurationSource) {
    String[] publicPaths = appProperties.security().publicPaths().toArray(String[]::new);
    return http.cors(cors -> cors.configurationSource(corsConfigurationSource))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .logout(ServerHttpSecurity.LogoutSpec::disable)
        .authorizeExchange(
            exchanges ->
                exchanges.pathMatchers(publicPaths).permitAll().anyExchange().authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .bearerTokenConverter(
                        exchange -> {
                          HttpCookie cookie =
                              exchange.getRequest().getCookies().getFirst("accessToken");
                          if (cookie == null) return Mono.empty();
                          return Mono.just(new BearerTokenAuthenticationToken(cookie.getValue()));
                        })
                    .jwt(jwt -> {}))
        .build();
  }

  @Bean
  public ReactiveJwtDecoder reactiveJwtDecoder(AppProperties appProperties) {
    byte[] keyBytes = appProperties.jwt().secretKey().getBytes();
    SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
  }
}
