package fr.esiee.app;


import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.jlama.JlamaChatModel;
import fr.esiee.app.config.LLMConfig;
import fr.esiee.app.config.LLMElem;
import fr.esiee.app.config.mapper.LLMElemMapper;
import fr.esiee.app.config.mapper.LLMConfigMapper;
import fr.esiee.app.services.ApiService;
import fr.esiee.app.services.DbService;
import io.helidon.common.GenericType;
import io.helidon.common.context.Contexts;
import io.helidon.dbclient.DbClient;
import io.helidon.logging.common.LogConfig;
import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.staticcontent.StaticContentService;

import java.util.Map;
import java.util.logging.Logger;

/**
 * The application main class.
 */
public class Main {

  private static final StaticContentService FRONT_STATIC_PATH =
          StaticContentService.builder("/static").welcomeFileName("index.html").build();

  private static final Logger logger = Logger.getLogger(Main.class.getName());

  private Main() {
  }

  public static void main(String[] args) {
    System.out.println( "Hello World!" );

    ChatLanguageModel model = JlamaChatModel.builder()
            .modelName("tjake/TinyLlama-1.1B-Chat-v1.0-Jlama-Q4")
            .temperature(0.3f)
            .build();

    String response = model.generate(
                    SystemMessage.from("You are helpful chatbot who is a java expert."),
                    UserMessage.from("Write a java program to print hello world."))
            .content()
            .text();

    System.out.println("\n" + response + "\n");
    LogConfig.configureRuntime();

    Config config = Config.builder()
            .addMapper(LLMElem.class, new LLMElemMapper())
            .addMapper(LLMConfig.class, new LLMConfigMapper())
            .build();
    Config.global(config);

    DbClient dbClient = DbClient.create(config.get("db"));
    Contexts.globalContext().register(dbClient);

    WebServer server = WebServer.builder().config(config.get("server")).routing(Main::routing).build().start();
    System.out.println("WEB server is up! http://localhost:" + server.port());

  }

  /**
   * Updates HTTP Routing.
   */
  static void routing(HttpRouting.Builder routing) {
    routing.register("/greet", new GreetService())
            .register("/db", new DbService())
            .register("/api", new ApiService())
            .get("/simple-greet", (req, res) -> res.send("Hello World!"));
    registerFrontEndRoutes(routing);
  }

  private static void registerFrontEndRoutes(HttpRouting.Builder routing) {
    routing.register("/", FRONT_STATIC_PATH).register("/about", FRONT_STATIC_PATH);
  }
}
