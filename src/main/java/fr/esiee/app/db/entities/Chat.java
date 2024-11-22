package fr.esiee.app.db.entities;

import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Represents a chat entity with an id, title, last activity timestamp, and LLM id.
 */
public record Chat(int id, String title, Timestamp lastActivity, int llmId) {


  /**
   * Constructs a new Chat record.
   *
   * @param id           the unique identifier of the chat, must be non-negative
   * @param title        the title of the chat, must not be null
   * @param lastActivity the timestamp of the last activity, must not be null
   * @param llmId        the unique identifier of the LLM, must be non-negative
   * @throws IllegalArgumentException if id or llmId is negative
   * @throws NullPointerException     if title or lastActivity is null
   */
  public Chat(int id, String title, Timestamp lastActivity, int llmId) {
    Objects.requireNonNull(title);
    Objects.requireNonNull(lastActivity);
    if (id < 0 || llmId < 0) {
      throw new IllegalArgumentException("id or llmId is negative");
    }
    this.id = id;
    this.title = title;
    this.lastActivity = Timestamp.from(lastActivity.toInstant().truncatedTo(ChronoUnit.MILLIS));
    this.llmId = llmId;
  }
}
