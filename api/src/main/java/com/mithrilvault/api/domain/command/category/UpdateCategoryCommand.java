package com.mithrilvault.api.domain.command.category;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCategoryCommand(
    @JsonIgnore String id,
    @Size(min = 1, max = 100) String name,
    @Size(max = 50) String icon,
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color,
    @JsonIgnore String ownerId) {}
