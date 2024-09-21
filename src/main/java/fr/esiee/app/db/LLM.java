package fr.esiee.app.db;

import java.util.Objects;

public record LLM(int id, String name, String model) {
  public LLM {
    Objects.requireNonNull(name);
    Objects.requireNonNull(model);
    if (id < 0) {
      throw new IllegalArgumentException("id is negative");
    }
  }
}
