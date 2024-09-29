package fr.esiee.app.dto;

public record LLMElemDTO(int id, String name, String model, String caracteristics) {

  public LLMElemDTO {
    if (name == null || model == null || caracteristics == null) {
      throw new IllegalArgumentException("name or model is null");
    }
    if(name.isBlank() || model.isBlank()){
      throw new IllegalArgumentException("name or model is empty");
    }
  }

}
