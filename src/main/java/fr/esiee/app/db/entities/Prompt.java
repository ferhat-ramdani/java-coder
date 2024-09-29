package fr.esiee.app.db.entities;

import java.util.Objects;

public record Prompt(int id, String message, AuthorType authorType,int chatId, int llmId) {
  public Prompt {
    Objects.requireNonNull(message);
    Objects.requireNonNull(authorType);
    if (id < 0 || llmId < 0 || chatId < 0) {
      throw new IllegalArgumentException("id is negative");
    }
  }
}
