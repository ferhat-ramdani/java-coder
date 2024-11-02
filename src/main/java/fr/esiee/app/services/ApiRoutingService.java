package fr.esiee.app.services;

import fr.esiee.app.Main;
import fr.esiee.app.utils.ErrorUtils;
import io.helidon.common.context.Contexts;

import io.helidon.http.Status;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * This class defines the API routing service for the application.
 * It sets up the routing rules for various API endpoints and handles
 * the OpenAPI definition for the service.
 */
@OpenAPIDefinition(
        info = @Info(
                title = "GPT for dev API",
                description = "Services for manipulating chats, prompts and llms"),
        servers = {
                @Server(
                        description = "localhost",
                        url = "http://localhost:8080")
        }
)
public class ApiRoutingService implements HttpService {


  /**
   * Configures the routing rules for the API endpoints.
   *
   * @param httpRules the HTTP rules to configure
   */
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
