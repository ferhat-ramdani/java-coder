package fr.esiee.app.services;

import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;

public class ApiService implements HttpService {

  @Override
  public void routing(HttpRules httpRules) {
    httpRules.get("/", (req, res) -> {
      res.send("API Service");
    }).register("/llm", new LLMService())
      .register("/chat", new ChatService())
      .register("/prompt", new PromptService());

  }
}
