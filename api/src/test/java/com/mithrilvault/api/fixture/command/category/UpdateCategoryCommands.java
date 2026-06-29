package com.mithrilvault.api.fixture.command.category;

import com.mithrilvault.api.domain.command.category.UpdateCategoryCommand;

public final class UpdateCategoryCommands {

  public static final String UPDATED_NAME = "Animals";

  private UpdateCategoryCommands() {}

  public static UpdateCategoryCommand withName() {
    return new UpdateCategoryCommand(UPDATED_NAME, null, null);
  }

  public static UpdateCategoryCommand withColor() {
    return new UpdateCategoryCommand(null, null, "#BF616A");
  }

  public static UpdateCategoryCommand full() {
    return new UpdateCategoryCommand(UPDATED_NAME, "🐱", "#BF616A");
  }
}
