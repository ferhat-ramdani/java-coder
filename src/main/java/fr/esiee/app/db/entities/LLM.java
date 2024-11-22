package fr.esiee.app.db.entities;

import java.util.Objects;

/**
 * Represents a Language Learning Model (LLM) with various attributes.
 */
public record LLM(int id, String name, String model, String systemPrompt, String characteristics, double temp, int seed,
                  int timeoutSec) {

  /**
   * Constructs an LLM record.
   *
   * @param id              the unique identifier of the LLM, must be non-negative
   * @param name            the name of the LLM, must not be null
   * @param model           the model of the LLM, must not be null
   * @param systemPrompt    the system prompt of the LLM, must not be null
   * @param characteristics the characteristics of the LLM, must not be null
   * @param temp            the temperature setting of the LLM
   * @param seed            the seed value for the LLM
   * @param timeoutSec      the timeout in seconds for the LLM
   * @throws IllegalArgumentException if id is negative
   * @throws NullPointerException     if name, model, systemPrompt, or characteristics are null
   */
  public LLM {
    Objects.requireNonNull(name);
    Objects.requireNonNull(model);
    Objects.requireNonNull(systemPrompt);
    Objects.requireNonNull(characteristics);
    if (id < 0) {
      throw new IllegalArgumentException("id is negative");
    }
  }
}
