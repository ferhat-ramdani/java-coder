package fr.esiee.app.llms;

import fr.esiee.app.db.entities.LLM;

import java.util.Objects;

/**
 * A DTO for the LLM entity.
 */
public record LLMDTO(int id, String name, String model, boolean installed) {

  /**
   * Constructs an LLMDTO and validates the required fields.
   *
   * @param id the unique identifier for the LLMDTO, must be non-negative
   * @param name the name of the LLMDTO, must not be null or blank
   * @param model the model of the LLMDTO, must not be null or blank
   * @param installed whether the model has already been downloaded locally
   * @throws NullPointerException     if name or model are null
   * @throws IllegalArgumentException if name or model are blank, or if id is negative
   */
  public LLMDTO {
    Objects.requireNonNull(name);
    Objects.requireNonNull(model);
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    if (model.isBlank()) {
      throw new IllegalArgumentException("model must not be blank");
    }
    if (id < 0) {
      throw new IllegalArgumentException("id must not be negative");
    }
  }

  /**
   * Creates a new LLMDTO from an existing LLM entity.
   *
   * @param llm the LLM entity to copy from
   * @param installed whether the model has already been downloaded locally
   * @return a new LLMDTO with the same properties as the given LLM entity
   */
  public static LLMDTO copyOf(LLM llm, boolean installed) {
    return new LLMDTO(llm.id(), llm.name(), llm.model(), installed);
  }

}
