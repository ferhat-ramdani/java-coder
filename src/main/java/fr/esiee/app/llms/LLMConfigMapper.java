package fr.esiee.app.utils.llms;

import io.helidon.config.Config;

import java.util.function.Function;

public class LLMConfigMapper implements Function<Config, LLMConfig> {

    @Override
    public LLMConfig apply(Config config) {
        return new LLMConfig(config.get("url").asString().get(),config.get("port").asInt().get());
    }
}
