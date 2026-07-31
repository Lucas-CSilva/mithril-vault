package com.mithrilvault.api.infrastructure.adapter.projection;

import com.mithrilvault.api.infrastructure.adapter.persistence.ProjectionLeaderRepositoryAdapter;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectionLeaderElector {

  private final ProjectionLeaderRepositoryAdapter leaderRepository;

  public Flux<Boolean> leadershipSignal(
      String projectionName, String instanceId, Duration leaseTtl) {
    return Flux.interval(Duration.ZERO, leaseTtl.dividedBy(3))
        .flatMap(tick -> leaderRepository.tryAcquireOrRenew(projectionName, instanceId, leaseTtl))
        .onErrorResume(ex -> Mono.just(false))
        .distinctUntilChanged()
        .doOnNext(
            isLeader ->
                log.info(
                    "Instance {} {} leadership for projection {}",
                    instanceId,
                    isLeader ? "acquired" : "lost",
                    projectionName))
        .startWith(false);
  }
}
