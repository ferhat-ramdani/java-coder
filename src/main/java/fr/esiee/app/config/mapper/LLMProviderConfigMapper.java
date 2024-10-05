package fr.esiee.app.config.mapper;

import fr.esiee.app.config.LLMProviderConfig;
import io.helidon.config.Config;

import java.util.function.Function;

public class LLMProviderConfigMapper implements Function<Config, LLMProviderConfig> {

    @Override
    public LLMProviderConfig apply(Config config) {
        return new LLMProviderConfig(config.get("url").asString().get(),config.get("port").asInt().get());
    }
}
