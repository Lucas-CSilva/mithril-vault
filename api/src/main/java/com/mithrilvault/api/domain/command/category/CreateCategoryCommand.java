package com.mithrilvault.api.domain.command.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCategoryCommand(
    @NotBlank @Size(max = 100) String name,
    String parentId,
    @Size(max = 50) String icon,
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color) {}
