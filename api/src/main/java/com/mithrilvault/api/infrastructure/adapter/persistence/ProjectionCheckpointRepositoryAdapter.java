package com.mithrilvault.api.infrastructure.adapter.persistence;

import com.mithrilvault.api.infrastructure.persistence.ProjectionCheckpointMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.document.ProjectionCheckpointDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProjectionCheckpointRepositoryAdapter {

  private final ProjectionCheckpointMongoRepository mongoRepository;

  public Mono<Document> findResumeToken(String projectionName) {
    return mongoRepository
        .findById(projectionName)
        .map(ProjectionCheckpointDocument::getResumeToken);
  }

  public Mono<Void> advance(
      String projectionName, Document resumeToken, String lastProcessedTransactionId) {
    return mongoRepository
        .findById(projectionName)
        .defaultIfEmpty(
            ProjectionCheckpointDocument.builder().projectionName(projectionName).build())
        .flatMap(
            checkpoint -> {
              checkpoint.update(resumeToken, lastProcessedTransactionId);
              return mongoRepository.save(checkpoint);
            })
        .doOnSuccess(
            checkpoint ->
                log.info(
                    "Checkpoint: {} saved for transaction: {}",
                    projectionName,
                    lastProcessedTransactionId))
        .then();
  }
}
