package fr.esiee.app.config.mapper;

import fr.esiee.app.config.LLMElem;
import io.helidon.config.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class LLMConfigMapper implements Function<Config, Map<String, LLMElem>> {

    @Override
    public Map<String, LLMElem> apply(Config config) {
        Map<String, LLMElem> llmsMap = new HashMap<>();
        List<Config> llmsList = config.asNodeList().orElse(List.of());
        for (Config llmItem : llmsList) {
            String llmName = llmItem.name();
            LLMElem llm = llmItem.as(LLMElem.class).orElse(null);
            if (llm != null) {
                llmsMap.put(llmName, llm);
            }
        }
        System.out.println(llmsMap);
        return llmsMap;
    }
}
