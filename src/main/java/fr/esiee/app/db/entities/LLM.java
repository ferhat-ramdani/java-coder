package fr.esiee.app.db.entities;

import java.util.Objects;

public record LLM(int id, String name, String model, String systemPrompt, String characteristics, double temp, int seed, int timeoutSec) {
  public LLM {
    Objects.requireNonNull(name);
    Objects.requireNonNull(model);
    Objects.requireNonNull(systemPrompt);
    Objects.requireNonNull(characteristics);
    if (id < 0) {
      throw new IllegalArgumentException("id is negative");
    }
  }
}
