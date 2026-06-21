package com.mithrilvault.api.infrastructure.config;

import com.mithrilvault.api.domain.config.AppProperties;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  @Profile("!local")
  public SecurityWebFilterChain securityWebFilterChain(
      ServerHttpSecurity http, AppProperties appProperties) {
    String[] publicPaths = appProperties.security().publicPaths().toArray(String[]::new);
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .logout(ServerHttpSecurity.LogoutSpec::disable)
        .authorizeExchange(
            exchanges ->
                exchanges.pathMatchers(publicPaths).permitAll().anyExchange().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
        .build();
  }

  @Bean
  @Profile("!local & !it")
  public ReactiveJwtDecoder reactiveJwtDecoder(AppProperties appProperties) {
    byte[] keyBytes = appProperties.jwt().secretKey().getBytes();
    SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
  }

  @Bean
  @Profile("local")
  public SecurityWebFilterChain localSecurityWebFilterChain(ServerHttpSecurity http) {
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
        .build();
  }
}
