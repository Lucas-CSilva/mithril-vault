package com.mithrilvault.api.fixture.model;

import com.mithrilvault.api.domain.model.Category;

public final class Categories {

  public static final String SYSTEM_ID = "system-alimentacao-fixture-1";
  public static final String DEFAULT_OWNER_ID = "owner-fixture-1";
  public static final String OTHER_OWNER_ID = "owner-fixture-2";

  private Categories() {}

  public static Category systemTopLevel() {
    return Category.builder()
        .id(SYSTEM_ID)
        .name("Alimentação")
        .icon("🍽️")
        .color("#FF7043")
        .isSystem(true)
        .ownerId(null)
        .build();
  }

  public static Category userTopLevel() {
    return Category.builder()
        .id("user-cat-fixture-1")
        .name("Pets")
        .icon("🐾")
        .color("#A3BE8C")
        .isSystem(false)
        .ownerId(DEFAULT_OWNER_ID)
        .build();
  }

  public static Category userTopLevel(String ownerId) {
    return userTopLevel().toBuilder().id("user-cat-fixture-" + ownerId).ownerId(ownerId).build();
  }

  public static Category userChild(String parentId) {
    return Category.builder()
        .id("user-cat-child-fixture-1")
        .name("Ração")
        .isSystem(false)
        .ownerId(DEFAULT_OWNER_ID)
        .parentId(parentId)
        .build();
  }

  public static Category userChild(String parentId, String ownerId) {
    return userChild(parentId).toBuilder().ownerId(ownerId).build();
  }
}
