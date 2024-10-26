package fr.esiee.app.utils.llms;

import fr.esiee.app.db.entities.LLM;

public record LLMDTO(int id, String name, String model, String caracteristics) {

  public LLMDTO {
    if (name == null || model == null || caracteristics == null) {
      throw new IllegalArgumentException("name or model is null");
    }
    if(name.isBlank() || model.isBlank()){
      throw new IllegalArgumentException("name or model is empty");
    }
  }

  public static LLMDTO copyOf(LLM llm) {
    return new LLMDTO(llm.id(), llm.name(), llm.model(), llm.caracteristics());
  }

}
