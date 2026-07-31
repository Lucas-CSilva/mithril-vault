package com.mithrilvault.api.infrastructure.adapter.persistence;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.mithrilvault.api.infrastructure.persistence.document.ProjectionLeaseDocument;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProjectionLeaderRepositoryAdapter {

  private final ReactiveMongoTemplate reactiveMongoTemplate;

  public Mono<Boolean> tryAcquireOrRenew(
      String projectionName, String instanceId, Duration leaseTtl) {

    // projectionName is the @Id field, mapped to _id by Spring Data — the query must filter on
    // "_id", not the raw field name, or it never matches the existing lease document and every
    // call past the first upserts a duplicate _id instead of renewing/contesting the lease.
    Query findAvailableLease =
        query(
            where("_id")
                .is(projectionName)
                .orOperator(
                    where(ProjectionLeaseDocument.Fields.instanceId).is(instanceId),
                    where(ProjectionLeaseDocument.Fields.leaseExpiresAt).lt(Instant.now())));

    Update updated =
        new Update()
            .set(ProjectionLeaseDocument.Fields.instanceId, instanceId)
            .set(ProjectionLeaseDocument.Fields.leaseExpiresAt, Instant.now().plus(leaseTtl));

    FindAndModifyOptions options = FindAndModifyOptions.options().upsert(true).returnNew(true);

    return reactiveMongoTemplate
        .findAndModify(findAvailableLease, updated, options, ProjectionLeaseDocument.class)
        .map(lease -> lease.getInstanceId().equals(instanceId))
        .defaultIfEmpty(false)
        // An upsert whose query doesn't match an existing lease (held by someone else, not yet
        // expired) still attempts to insert a doc with that same _id, which MongoDB rejects as
        // a duplicate key rather than reporting "no match" — that IS the "lost the race" case.
        .onErrorResume(DuplicateKeyException.class, ex -> Mono.just(false))
        .doOnNext(
            acquired -> {
              if (!acquired) {
                log.info(
                    "Instance {} lost the race for lease on projection {}",
                    instanceId,
                    projectionName);
              }
            });
  }
}
