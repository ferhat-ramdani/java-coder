package fr.esiee.app.config.mapper;

import fr.esiee.app.config.LLMElem;
import io.helidon.config.Config;

import java.util.function.Function;

public class LLMElemMapper implements Function<Config, LLMElem> {
  @Override
  public LLMElem apply(Config config) {
    return new LLMElem(config.get("name").asString().get(),
        config.get("model").asString().get(),
        config.get("system_prompt").asString().get());
//            config.get("models").asList(String.class).get());
  }
}
