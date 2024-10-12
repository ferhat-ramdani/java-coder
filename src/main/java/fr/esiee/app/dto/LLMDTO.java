package fr.esiee.app.dto;

public record LLMDTO(int id, String name, String model, String caracteristics) {

  public LLMDTO {
    if (name == null || model == null || caracteristics == null) {
      throw new IllegalArgumentException("name or model is null");
    }
    if(name.isBlank() || model.isBlank()){
      throw new IllegalArgumentException("name or model is empty");
    }
  }

}
