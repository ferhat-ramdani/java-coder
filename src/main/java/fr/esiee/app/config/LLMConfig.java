package fr.esiee.app.config;

import java.util.List;

public record LLMConfig(String provider, String url, int port, List<LLMElem> models) {
}
