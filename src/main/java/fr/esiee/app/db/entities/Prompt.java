package fr.esiee.app.db.entities;

import java.util.Objects;

public record Prompt(Integer id, String message, AuthorType authorType, int chatId, boolean compile) {
  public Prompt {
    Objects.requireNonNull(id);
    Objects.requireNonNull(message);
    Objects.requireNonNull(authorType);
    if (id < 0  || chatId < 0) {
      throw new IllegalArgumentException("id is negative");
    }
  }
}
