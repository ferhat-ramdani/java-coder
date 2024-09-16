package fr.esiee.app;


import io.helidon.common.context.Contexts;
import io.helidon.dbclient.DbClient;
import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;
import io.helidon.http.Status;
import io.helidon.logging.common.LogConfig;
import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.staticcontent.StaticContentService;


/**
 * The application main class.
 */
public class Main {


  /**
   * Cannot be instantiated.
   */
  private Main() {
  }


  /**
   * Application main entry point.
   *
   * @param args command line arguments.
   */
  public static void main(String[] args) {

    // load logging configuration
    LogConfig.configureRuntime();

    // initialize global config from default configuration
    Config config = Config.create();
    Config.global(config);

    DbClient dbClient = DbClient.create(config.get("db"));
    Contexts.globalContext().register(dbClient);


    WebServer server = WebServer.builder()
            .config(config.get("server"))
            .routing(Main::routing)
            .build()
            .start();


    System.out.println("WEB server is up! http://localhost:" + server.port() + "/simple-greet");

  }


  /**
   * Updates HTTP Routing.
   */
  static void routing(HttpRouting.Builder routing) {
    routing
            .register("/greet", new GreetService())
            .register("/db", new DbService())
            .get("/simple-greet", (req, res) -> res.send("Hello World!"))
            .register("/", StaticContentService.builder("/static")
                    .welcomeFileName("index.html")
                    .build())
            .any("/*", (req, res) -> {
              System.out.println(1);
              res.status(Status.MOVED_PERMANENTLY_301);
              res.header(HeaderValues.createCached(HeaderNames.LOCATION, "/"));
              res.send();
            });
  }


}
