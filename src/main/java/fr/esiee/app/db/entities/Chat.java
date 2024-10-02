package fr.esiee.app.db.entities;

import java.sql.Timestamp;
import java.util.Objects;

public record Chat(int id, String title, Timestamp lastActivity, int llmId) {
  public Chat {
    Objects.requireNonNull(title);
    Objects.requireNonNull(lastActivity);
    if (id < 0 || llmId < 0) {
      throw new IllegalArgumentException("id or llmId is negative");
    }
  }
}
