package fr.esiee.app.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

/**
 * Represents a prompt entity.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Prompt(int id, String message, AuthorType authorType, int chatId, boolean compile) {


  /**
   * Constructs a new Prompt record.
   *
   * @param message    the message of the prompt, must not be null
   * @param authorType the type of the author, must not be null
   * @param id         the id of the prompt, must be non-negative
   * @param chatId     the id of the chat, must be non-negative
   * @param compile    whether the prompt should be compiled
   * @throws IllegalArgumentException if id or chatId is negative
   * @throws NullPointerException     if message or authorType is null
   */
  public Prompt {
    Objects.requireNonNull(message);
    Objects.requireNonNull(authorType);
    if (id < 0 || chatId < 0) {
      throw new IllegalArgumentException("id is negative");
    }
  }
}
