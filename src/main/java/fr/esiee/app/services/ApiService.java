package fr.esiee.app.services;

import fr.esiee.app.Main;
import io.helidon.common.context.Contexts;
import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;
import io.helidon.http.Status;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;

public class ApiService implements HttpService {

  @Override
  public void routing(HttpRules httpRules) {
    httpRules.register("/llm", new LLMService())
            .register("/chat", new ChatService())
            .register("/gen", new GeneratorService())
            .register("/prompt", new PromptService());

    if (Main.isDebugMode()) {
      httpRules.get("/stop", (req, res) -> {
        res.send("Stopping server...");
        Contexts.globalContext().get(WebServer.class).orElseThrow().stop();
      });
    }

    httpRules.any((_, res) -> res.header(HeaderValues.create(HeaderNames.CONTENT_TYPE, "application/json")).status(Status.NOT_FOUND_404)
            .send("{\"error\":\"Not found\"}"));
  }
}
