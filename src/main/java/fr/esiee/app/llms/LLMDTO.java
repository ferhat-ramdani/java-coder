package fr.esiee.app.llms;

import fr.esiee.app.db.entities.LLM;

import java.util.Objects;

public record LLMDTO(int id, String name, String model, String characteristics) {

  public LLMDTO {
    Objects.requireNonNull(name);
    Objects.requireNonNull(model);
    Objects.requireNonNull(characteristics);
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    if (model.isBlank()) {
      throw new IllegalArgumentException("model must not be blank");
    }
    if (characteristics.isBlank()) {
      throw new IllegalArgumentException("characteristics must not be blank");
    }
    if(id < 0) {
      throw new IllegalArgumentException("id must not be negative");
    }
  }

  public static LLMDTO copyOf(LLM llm) {
    return new LLMDTO(llm.id(), llm.name(), llm.model(), llm.characteristics());
  }

}
