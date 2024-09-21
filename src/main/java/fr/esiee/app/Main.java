package fr.esiee.app;


import io.helidon.common.context.Contexts;
import io.helidon.dbclient.DbClient;
import io.helidon.logging.common.LogConfig;
import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.staticcontent.StaticContentService;

/**
 * The application main class.
 */
public class Main {

  private static final StaticContentService FRONT_STATIC_PATH =
          StaticContentService.builder("/static").welcomeFileName("index.html").build();

  private Main() {
  }

  public static void main(String[] args) {

    LogConfig.configureRuntime();

    Config config = Config.create();
    Config.global(config);

    DbClient dbClient = DbClient.create(config.get("db"));
    Contexts.globalContext().register(dbClient);

    WebServer server = WebServer.builder().config(config.get("server")).routing(Main::routing).build().start();
    System.out.println("WEB server is up! http://localhost:" + server.port() + "/simple-greet");

  }

  /**
   * Updates HTTP Routing.
   */
  static void routing(HttpRouting.Builder routing) {
    routing.register("/greet", new GreetService()).register("/db", new DbService())
            .get("/simple-greet", (req, res) -> res.send("Hello World!"));
    registerFrontEndRoutes(routing);
  }

  private static void registerFrontEndRoutes(HttpRouting.Builder routing) {
    routing.register("/", FRONT_STATIC_PATH).register("/about", FRONT_STATIC_PATH);
  }
}
