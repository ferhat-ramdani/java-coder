package fr.esiee.app.db.entities;

import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record Chat(int id, String title, Timestamp lastActivity, int llmId) {
  private static final ChronoUnit TRUNCATION_UNIT = ChronoUnit.MILLIS;

  public Chat {
    Objects.requireNonNull(title);
    Objects.requireNonNull(lastActivity);
    if (id < 0 || llmId < 0) {
      throw new IllegalArgumentException("id or llmId is negative");
    }
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Chat chat && id == chat.id && llmId == chat.llmId && Objects.equals(title, chat.title) &&
            Objects.equals(lastActivity.toLocalDateTime().truncatedTo(TRUNCATION_UNIT),
                    chat.lastActivity.toLocalDateTime().truncatedTo(TRUNCATION_UNIT));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, title, lastActivity.toLocalDateTime().truncatedTo(TRUNCATION_UNIT), llmId);
  }
}
