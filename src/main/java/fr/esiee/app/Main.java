package fr.esiee.app;


import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import fr.esiee.app.config.LLMConfig;
import fr.esiee.app.config.mapper.LLMConfigMapper;
import fr.esiee.app.llmcheck.OllamaCheck;
import fr.esiee.app.services.ApiService;
import io.helidon.common.context.Contexts;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.http.media.jackson.JacksonSupport;
import io.helidon.logging.common.LogConfig;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.accesslog.AccessLogFeature;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
  private Main() {
  }

  public static void main(String[] args) throws IOException, InterruptedException {

    LogConfig.configureRuntime();

    Config config = Config.builder()
            .addMapper(LLMConfig.class, new LLMConfigMapper())
            .build();
    Config.global(config);


    DbClient dbClient = DbClient.create(config.get("db"));
    Contexts.globalContext().register(dbClient);

    OllamaCheck.init();

    /*    ChatLanguageModel model =
            OllamaChatModel.builder().baseUrl("http://localhost:11434").modelName("codellama:7b")
                    .build();

    // Example usage
    var answer = model.generate(new SystemMessage("Respond only with java code, don't explain, just give the code. You must have a main method in your class."),
            new UserMessage("current time"));
    System.out.println(answer.content().text());*/


    WebServer server = WebServer.builder()
            .mediaContext(it -> it
                    .mediaSupportsDiscoverServices(false)
                    .addMediaSupport(JacksonSupport.create(config))
                    .build())
            .addFeature(AccessLogFeature.builder()
                    .commonLogFormat()
                    .build())
                    .config(config.get("server"))
                    .routing(Main::routing).build().start();
    System.out.println("WEB server is up! http://localhost:" + server.port());

  }

  /**
   * Updates HTTP Routing.
   */
  static void routing(HttpRouting.Builder routing) {
    routing.register("/api", new ApiService());
    registerFrontEndRoutes(routing);
  }

  private static void registerFrontEndRoutes(HttpRouting.Builder routing) {
    routing.register("/", FRONT_STATIC_PATH).register("/about", FRONT_STATIC_PATH);
  }
}
