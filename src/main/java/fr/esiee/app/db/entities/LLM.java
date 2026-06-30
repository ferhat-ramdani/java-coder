package fr.esiee.app.db.entities;

import java.util.Objects;

/**
 * Represents a Language Learning Model (LLM) with various attributes.
 */
public record LLM(int id, String name, String model, String systemPrompt, double temp, int seed,
                  int timeoutSec) {

  /**
   * Constructs an LLM and validates the required fields.
   *
   * @param id the unique identifier for the LLM, must be non-negative
   * @param name the name of the LLM, must not be null or blank
   * @param model the model of the LLM, must not be null or blank
   * @param systemPrompt the system prompt for the LLM, must not be null
   * @param temp the temperature setting for the LLM
   * @param seed the seed setting for the LLM
   * @param timeoutSec the timeout in seconds for the LLM
   * @throws NullPointerException     if name, model, or systemPrompt are null
   * @throws IllegalArgumentException if name or model are blank, or if id is negative
   */
  public LLM {
    Objects.requireNonNull(name);
    Objects.requireNonNull(model);
    Objects.requireNonNull(systemPrompt);
    if (id < 0) {
      throw new IllegalArgumentException("id is negative");
    }
  }
}
