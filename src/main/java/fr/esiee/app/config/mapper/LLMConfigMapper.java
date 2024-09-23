package fr.esiee.app.config.mapper;

import fr.esiee.app.config.LLMConfig;
import fr.esiee.app.config.LLMElem;
import io.helidon.config.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class LLMConfigMapper implements Function<Config, LLMConfig> {

    @Override
    public LLMConfig apply(Config config) {
        return new LLMConfig(config.get("provider").asString().get(),
                config.get("url").asString().get(),
                config.get("port").asInt().get(),
                config.get("models").asList(LLMElem.class).get());
    }
}
