package com.mithrilvault.api.infrastructure.persistence;

import com.mithrilvault.api.infrastructure.persistence.document.ProjectionCheckpointDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface ProjectionCheckpointMongoRepository
    extends ReactiveMongoRepository<ProjectionCheckpointDocument, String> {}
