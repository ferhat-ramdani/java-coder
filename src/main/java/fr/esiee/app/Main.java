package fr.esiee.app;


import fr.esiee.app.config.LLMConfig;
import fr.esiee.app.config.mapper.LLMConfigMapper;
import fr.esiee.app.db.DbManager;
import fr.esiee.app.utils.ErrorUtils;
import fr.esiee.app.exception.RestApiException;
import fr.esiee.app.llms.OllamaCheck;
import fr.esiee.app.services.ApiRoutingService;
import io.helidon.common.context.Contexts;
import io.helidon.config.Config;
import io.helidon.cors.CrossOriginConfig;
import io.helidon.http.NotFoundException;
import io.helidon.http.Status;
import io.helidon.http.media.jackson.JacksonSupport;
import io.helidon.logging.common.LogConfig;
import io.helidon.openapi.OpenApiFeature;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.cors.CorsSupport;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.staticcontent.StaticContentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {

  private static final StaticContentService FRONT_STATIC_PATH =
          StaticContentService.builder("/static").welcomeFileName("index.html").build();
  private static final String DB_DEFAULT_USER = "gptfordev";
  private static final String DB_DEFAULT_PASSWORD = "gptfordev";
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  public static boolean isDebugMode() {
    return Config.global().get("debug").asBoolean().orElse(false);
  }

  public static void main(String[] args) throws IOException, InterruptedException {

    LogConfig.configureRuntime();

    var config = Config.builder()
            .addMapper(LLMConfig.class, new LLMConfigMapper())
            .build();
    Config.global(config);

    var dbUser = config.get("db.connection.username").asString().orElseThrow(() -> new RuntimeException("Database username is not set."));
    var dbPassword = config.get("db.connection.password").asString().orElseThrow(() -> new RuntimeException("Database password is not set."));

    if (dbUser.isBlank() || dbUser.isEmpty()) {
      LOGGER.error("Database username is not set.");
      System.exit(1);
    }

    if (dbPassword.isBlank() || dbPassword.isEmpty()) {
      LOGGER.error("Database password is not set.");
      System.exit(1);
    }

    if (dbUser.equals(DB_DEFAULT_USER) || dbPassword.equals(DB_DEFAULT_PASSWORD)) {
      LOGGER.warn("You are using the default database user or password.");
      LOGGER.warn("To change it, you can set the values in the `application.yaml` file.");
      LOGGER.warn("Or in the environment variables. By set `db_connection_username` and `db_connection_password` values.");
    }

    DbManager.initialize();

    var llmConfig = config.get("provider").as(LLMConfig.class).orElse(LLMConfig.defaultConfig());
    Contexts.globalContext().register(llmConfig);

    OllamaCheck.initOllamaAndLLMs();

    var server = WebServer.builder()
            .mediaContext(it -> it
                    .mediaSupportsDiscoverServices(false)
                    .addMediaSupport(JacksonSupport.create(config))
                    .build())
            .addFeature(OpenApiFeature.create(config.get("openapi")))
            .config(config.get("server"))
            .routing(Main::routing).build().start();

    Contexts.globalContext().register(server);

    LOGGER.info("WEB server is up! http://localhost:{}", server.port());

  }

  static void routing(HttpRouting.Builder routing) {

    if (isDebugMode()) {
      CorsSupport corsSupport = CorsSupport.builder()
              .addCrossOrigin(CrossOriginConfig.builder()
                      .allowOrigins("*")
                      .allowMethods("*")
                      .build())
              .addCrossOrigin(CrossOriginConfig.create())
              .build();
      routing.register("/api", corsSupport, new ApiRoutingService());
    } else {
      routing.register("/api", new ApiRoutingService());
    }

    routing.error(NotFoundException.class, (_, res, exception) -> {
      ErrorUtils.send(res, Status.BAD_REQUEST_400, exception.getMessage());
    }).error(RestApiException.class, (_, res, exception) -> {
      ErrorUtils.send(res, Status.INTERNAL_SERVER_ERROR_500, exception.getMessage());
    });

    registerFrontEndRoutes(routing);
  }

  private static void registerFrontEndRoutes(HttpRouting.Builder routing) {
    routing.register("/", FRONT_STATIC_PATH).register("/about", FRONT_STATIC_PATH);
  }
}
