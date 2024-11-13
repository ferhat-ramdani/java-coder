package fr.esiee.app.db.entities;

import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record Chat(int id, String title, Timestamp lastActivity, int llmId) {

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
