package com.mithrilvault.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("it")
public abstract class AbstractIntegrationTest {

  @Container static MongoDBContainer mongodb = new MongoDBContainer("mongo:8");

  @DynamicPropertySource
  static void mongoProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.mongodb.uri", mongodb::getReplicaSetUrl);
  }

  // Prevents Spring from connecting to a real auth server on context startup.
  // Use webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt()) for authenticated calls.
  @MockitoBean ReactiveJwtDecoder jwtDecoder;

  @Autowired protected WebTestClient webTestClient;
}
