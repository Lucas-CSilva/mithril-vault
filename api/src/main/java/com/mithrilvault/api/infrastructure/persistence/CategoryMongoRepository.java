package com.mithrilvault.api.infrastructure.persistence;

import com.mithrilvault.api.infrastructure.persistence.document.CategoryDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface CategoryMongoRepository
    extends ReactiveMongoRepository<CategoryDocument, String> {}
