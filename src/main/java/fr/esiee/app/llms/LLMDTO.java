package fr.esiee.app.llms;

import fr.esiee.app.db.entities.LLM;

public record LLMDTO(int id, String name, String model, String characteristics) {

  public LLMDTO {
    if (name == null || model == null || characteristics == null) {
      throw new IllegalArgumentException("name or model is null");
    }
    if(name.isBlank() || model.isBlank() || characteristics.isBlank()) {
      throw new IllegalArgumentException("name or model or characteristics is empty");
    }
  }

  public static LLMDTO copyOf(LLM llm) {
    return new LLMDTO(llm.id(), llm.name(), llm.model(), llm.characteristics());
  }

}
