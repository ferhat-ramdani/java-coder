package fr.esiee.app;

import fr.esiee.app.config.LLMConfig;
import fr.esiee.app.config.mapper.LLMConfigMapper;
import fr.esiee.app.db.DbManager;
import fr.esiee.app.exception.RestApiException;
import fr.esiee.app.llms.OllamaSetupManager;
import fr.esiee.app.services.ApiRoutingService;
import fr.esiee.app.utils.ErrorUtils;
import io.helidon.common.context.Contexts;
import io.helidon.config.Config;
import io.helidon.http.NotFoundException;
import io.helidon.http.Status;
import io.helidon.http.media.jackson.JacksonSupport;
import io.helidon.logging.common.LogConfig;
import io.helidon.openapi.OpenApiFeature;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.staticcontent.StaticContentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * The main class for the application.
 */
public class Main {

  // We don't have injection, so we need to use this declaration.
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  /**
   * Checks if the application is running in debug mode.
   *
   * @return true if debug mode is enabled, false otherwise
   */
  public static boolean isDebugMode() {
    return Config.global().get("debug").asBoolean().orElse(false);
  }

  /**
   * Creates and starts a new WebServer instance.
   *
   * @param config the configuration to use for the WebServer
   * @return the created WebServer instance
   */
  private static WebServer createWebServer(Config config) {

    return WebServer.builder()
            .mediaContext(it -> it
                    .mediaSupportsDiscoverServices(false)
                    .addMediaSupport(JacksonSupport.create(config))
                    .build())
            .addFeature(OpenApiFeature.builder().build())
            .config(config.get("server"))
            .routing(Main::routing).build().start();
  }

  /**
   * The main method that serves as the entry point for the application.
   *
   * @param args the command line arguments
   * @throws IOException          if an I/O error occurs
   * @throws InterruptedException if the thread is interrupted
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    LogConfig.configureRuntime();

    var config = Config.builder()
            .addMapper(LLMConfig.class, new LLMConfigMapper())
            .build();

    Config.global(config);
    DbManager.initialize();
    var llmConfig = config.get("provider").as(LLMConfig.class).orElse(LLMConfig.defaultConfig());
    Contexts.globalContext().register(llmConfig);
    OllamaSetupManager.setupAndStartOllama();

    var server = createWebServer(config);

    Contexts.globalContext().register(server);
    LOGGER.info("WEB server is up! {}://{}:{}", server.hasTls() ? "https" : "http", server.prototype().host(),
            server.port());
  }

  /**
   * Configures the routing for the web server.
   *
   * @param routing the HttpRouting.Builder to configure the routes with
   */
  public static void routing(HttpRouting.Builder routing) {
    routing.register("/api", new ApiRoutingService());
    routing.error(NotFoundException.class, (_, res, exception) -> {
              ErrorUtils.send(res, Status.BAD_REQUEST_400, exception.getMessage());
              LOGGER.error("A NotFoundException occurred: ", exception);
            })
            .error(RestApiException.class, (_, res, exception) -> {
              ErrorUtils.send(res, Status.INTERNAL_SERVER_ERROR_500, exception.getMessage());
              LOGGER.error("A RestApiException occurred: ", exception);
            });
    registerFrontEndRoutes(routing);
  }

  /**
   * Registers the front-end routes for the web server.
   *
   * @param routing the HttpRouting.Builder to register the routes with
   */
  private static void registerFrontEndRoutes(HttpRouting.Builder routing) {
    var frontStaticPath =
            StaticContentService.builder("static").welcomeFileName("index.html").build();

    routing.register("/", frontStaticPath)
            .register("/chats[/*]", frontStaticPath)
            .register("/llms", frontStaticPath);
  }
}
