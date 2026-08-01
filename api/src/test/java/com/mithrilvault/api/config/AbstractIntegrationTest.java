package com.mithrilvault.api.config;

import com.mithrilvault.api.steps.AccountSteps;
import com.mithrilvault.api.steps.CategorySteps;
import com.mithrilvault.api.steps.TransactionSteps;
import com.mithrilvault.api.steps.UserSteps;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("it")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

  static final MongoDBContainer mongodb = new MongoDBContainer("mongo:8").withReplicaSet();

  protected static final LocalStackContainer localstack =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.7.0"))
          .withServices(LocalStackContainer.Service.SQS);

  static {
    mongodb.start();
    localstack.start();
  }

  @DynamicPropertySource
  static void mongoProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.mongodb.uri", () -> mongodb.getReplicaSetUrl() + "?directConnection=true");
  }

  @DynamicPropertySource
  static void awsProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.cloud.aws.region.static", localstack::getRegion);
    registry.add("spring.cloud.aws.credentials.access-key", localstack::getAccessKey);
    registry.add("spring.cloud.aws.credentials.secret-key", localstack::getSecretKey);
    registry.add(
        "spring.cloud.aws.sqs.endpoint",
        () -> localstack.getEndpointOverride(LocalStackContainer.Service.SQS).toString());
  }

  @LocalServerPort private int port;

  protected WebTestClient webTestClient;
  protected UserSteps userSteps = new UserSteps();
  protected CategorySteps categorySteps = new CategorySteps();
  protected AccountSteps accountSteps = new AccountSteps();
  protected TransactionSteps transactionSteps = new TransactionSteps();

  @BeforeEach
  void initWebTestClient() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    userSteps.init(webTestClient);
    categorySteps.init(webTestClient);
    accountSteps.init(webTestClient);
    transactionSteps.init(webTestClient);
  }
}
