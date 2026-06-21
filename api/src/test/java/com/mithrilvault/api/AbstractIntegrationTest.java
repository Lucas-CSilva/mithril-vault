package com.mithrilvault.api;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
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

  @LocalServerPort private int port;

  @MockitoBean ReactiveJwtDecoder jwtDecoder;

  protected WebTestClient webTestClient;

  @BeforeEach
  void initWebTestClient() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }
}
