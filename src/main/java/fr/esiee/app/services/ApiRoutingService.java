package fr.esiee.app.services;

import fr.esiee.app.Main;
import fr.esiee.app.utils.ErrorUtils;
import io.helidon.common.context.Contexts;

import io.helidon.http.Status;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;

public class ApiRoutingService implements HttpService {

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

    httpRules.any((_, res) -> ErrorUtils.send(res, Status.BAD_REQUEST_400, "Invalid endpoint"));
  }
}
