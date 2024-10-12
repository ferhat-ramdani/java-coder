package fr.esiee.app.db.entities;

import java.util.Objects;

public record LLM(int id, String name, String model, String systemPrompt, String caracteristics, Double temp, int seed) {
  public LLM {
    Objects.requireNonNull(name);
    Objects.requireNonNull(model);
    Objects.requireNonNull(caracteristics);
    if (id < 0) {
      throw new IllegalArgumentException("id is negative");
    }
  }
}
