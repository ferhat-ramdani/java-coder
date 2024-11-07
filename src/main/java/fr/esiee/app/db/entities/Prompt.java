package fr.esiee.app.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Prompt(int id, String message, AuthorType authorType, int chatId, boolean compile) {
  public Prompt {
    Objects.requireNonNull(message);
    Objects.requireNonNull(authorType);
    if (id < 0  || chatId < 0) {
      throw new IllegalArgumentException("id is negative");
    }
  }
}
