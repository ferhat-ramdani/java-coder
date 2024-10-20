package fr.esiee.app;


import fr.esiee.app.config.LLMProviderConfig;
import fr.esiee.app.config.mapper.LLMProviderConfigMapper;
import fr.esiee.app.errors.ErrorUtils;
import fr.esiee.app.llmcheck.OllamaCheck;
import fr.esiee.app.services.ApiService;
import io.helidon.common.context.Contexts;
import io.helidon.config.Config;
import io.helidon.cors.CrossOriginConfig;
import io.helidon.dbclient.DbClient;
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

/**
 * The application main class.
 */
public class Main {

  private static final StaticContentService FRONT_STATIC_PATH =
          StaticContentService.builder("/static").welcomeFileName("index.html").build();

  private static final String BDD_DEFAULT_USER = "gptfordev";
  private static final String BDD_DEFAULT_PASSWORD = "gptfordev";

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);


  private Main() {
  }

  public static boolean isDebugMode() {
    return Config.global().get("debug").asBoolean().orElse(false);
  }

  public static void main(String[] args) throws IOException, InterruptedException {

    LogConfig.configureRuntime();

    var config = Config.builder()
            .addMapper(LLMProviderConfig.class, new LLMProviderConfigMapper())
            .build();
    Config.global(config);

    var bddUser = config.get("db.connection.username").asString().orElseThrow(() -> new RuntimeException("Database username is not set."));
    var bddPassword = config.get("db.connection.password").asString().orElseThrow(() -> new RuntimeException("Database password is not set."));

    if (bddUser.isBlank() || bddUser.isEmpty()) {
      LOGGER.error("Database username is not set.");
      System.exit(1);
    }

    if (bddPassword.isBlank() || bddPassword.isEmpty()) {
      LOGGER.error("Database password is not set.");
      System.exit(1);
    }

    if (bddUser.equals(BDD_DEFAULT_USER) || bddPassword.equals(BDD_DEFAULT_PASSWORD)) {
      LOGGER.warn("You are using the default database user or password.");
      LOGGER.warn("To change it, you can set the values in the `application.yaml` file.");
      LOGGER.warn("Or in the environment variables. By set `db_connection_username` and `db_connection_password` values.");
    }

    var dbClient = DbClient.create(config.get("db"));
    Contexts.globalContext().register(dbClient);

    var llmConfig = config.get("provider").as(LLMProviderConfig.class).orElse(LLMProviderConfig.defaultConfig());
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

  /**
   * Updates HTTP Routing.
   */
  static void routing(HttpRouting.Builder routing) {

    if (isDebugMode()) {
      CorsSupport corsSupport = CorsSupport.builder()
              .addCrossOrigin(CrossOriginConfig.builder()
                      .allowOrigins("*")
                      .allowMethods("*")
                      .build())
              .addCrossOrigin(CrossOriginConfig.create())
              .build();
      routing.register("/api", corsSupport, new ApiService());
    } else {
      routing.register("/api", new ApiService());
    }


    routing.error(NotFoundException.class, (req, res, ex) -> {
      ErrorUtils.send(res, Status.BAD_REQUEST_400, ex.getMessage());
    });

    registerFrontEndRoutes(routing);
  }

  private static void registerFrontEndRoutes(HttpRouting.Builder routing) {
    routing.register("/", FRONT_STATIC_PATH).register("/about", FRONT_STATIC_PATH);
  }
}
