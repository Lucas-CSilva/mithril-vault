package com.mithrilvault.api.fixture.command.category;

import com.mithrilvault.api.domain.command.category.CreateCategoryCommand;

public final class CreateCategoryCommands {

  public static final String DEFAULT_NAME = "Pets";
  public static final String DEFAULT_ICON = "🐾";
  public static final String DEFAULT_COLOR = "#A3BE8C";

  private CreateCategoryCommands() {}

  public static CreateCategoryCommand topLevel() {
    return new CreateCategoryCommand(DEFAULT_NAME, null, DEFAULT_ICON, DEFAULT_COLOR);
  }

  public static CreateCategoryCommand withParent(String parentId) {
    return new CreateCategoryCommand("Ração", parentId, null, null);
  }

  public static CreateCategoryCommand withName(String name) {
    return new CreateCategoryCommand(name, null, DEFAULT_ICON, DEFAULT_COLOR);
  }
}
