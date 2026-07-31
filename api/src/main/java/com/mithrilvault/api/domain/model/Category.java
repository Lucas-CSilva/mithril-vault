package com.mithrilvault.api.domain.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record Category(
    String id,
    String name,
    String parentId,
    String icon,
    String color,
    boolean isSystem,
    String ownerId,
    Long version) {}
