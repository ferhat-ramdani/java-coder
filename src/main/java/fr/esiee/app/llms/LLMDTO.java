package fr.esiee.app.llms;

import fr.esiee.app.db.entities.LLM;

import java.util.Objects;

/**
 * A DTO for the LLM entity.
 */
public record LLMDTO(int id, String name, String model, String characteristics) {

  /**
   * Constructs a new LLMDTO record.
   *
   * @param id              the unique identifier of the LLMDTO, must not be negative
   * @param name            the name of the LLMDTO, must not be null or blank
   * @param model           the model of the LLMDTO, must not be null or blank
   * @param characteristics the characteristics of the LLMDTO, must not be null or blank
   * @throws NullPointerException     if name, model, or characteristics are null
   * @throws IllegalArgumentException if name, model, or characteristics are blank, or if id is negative
   */
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
    if (id < 0) {
      throw new IllegalArgumentException("id must not be negative");
    }
  }

  /**
   * Creates a new LLMDTO from an existing LLM entity.
   *
   * @param llm the LLM entity to copy from
   * @return a new LLMDTO with the same properties as the given LLM entity
   */
  public static LLMDTO copyOf(LLM llm) {
    return new LLMDTO(llm.id(), llm.name(), llm.model(), llm.characteristics());
  }

}
