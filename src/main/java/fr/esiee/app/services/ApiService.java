package fr.esiee.app.services;

import fr.esiee.app.Main;
import io.helidon.common.context.Contexts;
import io.helidon.cors.CrossOriginConfig;
import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;
import io.helidon.http.Status;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.cors.CorsSupport;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;

public class ApiService implements HttpService {

  @Override
  public void routing(HttpRules httpRules) {
    CorsSupport corsSupport = CorsSupport.builder()
            .addCrossOrigin(CrossOriginConfig.builder()
                    .allowOrigins("*")
                    .allowMethods("*")
                    .build())
            .addCrossOrigin(CrossOriginConfig.create())
            .build();
    httpRules.register("/llm", corsSupport, new LLMService())
            .register("/chat", corsSupport, new ChatService())
            .register("/gen", corsSupport, new GeneratorService())
      .register("/prompt", corsSupport, new PromptService());

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
