package fr.esiee.app.dto;

import java.util.List;

public record LLMElemDTO(String name, String model) {

  public LLMElemDTO {
    if (name == null || model == null) {
      throw new IllegalArgumentException("name or model is null");
    }
    if(name.isBlank() || model.isBlank()){
      throw new IllegalArgumentException("name or model is empty");
    }
  }

}
