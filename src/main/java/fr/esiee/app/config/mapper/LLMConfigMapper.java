package fr.esiee.app.config.mapper;

import fr.esiee.app.config.LLMConfig;
import io.helidon.config.Config;

import java.util.function.Function;

/**
 * A mapper class that converts Helidon Config instances to LLMConfig instances.
 */
public class LLMConfigMapper implements Function<Config, LLMConfig> {

  /**
   * Applies the configuration mapping from Helidon Config to LLMConfig.
   *
   * @param config the Helidon Config instance
   * @return a new LLMConfig instance with the mapped values
   */
  @Override
  public LLMConfig apply(Config config) {
    return new LLMConfig(config.get("url").asString().get(), config.get("port").asInt().get(), config.get("version").asString().get());
  }
}
