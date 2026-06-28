package com.mithrilvault.api.application.response;

public record CategoryResponse(
    String id, String name, String parentId, String icon, String color, boolean isSystem) {}
