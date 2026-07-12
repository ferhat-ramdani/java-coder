package fr.esiee.app.services;

import fr.esiee.app.db.DbManager;
import fr.esiee.app.db.entities.AuthorType;
import fr.esiee.app.db.entities.Chat;
import fr.esiee.app.db.entities.Prompt;
import fr.esiee.app.llms.OllamaSetupManager;
import fr.esiee.tests.DbUtils;
import io.helidon.common.context.Contexts;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.sse.SseSource;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.concurrent.CountDownLatch;

import static io.helidon.common.media.type.MediaTypes.TEXT_EVENT_STREAM;
import static io.helidon.http.HeaderValues.ACCEPT_EVENT_STREAM;
import static io.helidon.http.HttpMediaTypes.PLAINTEXT_UTF_8;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@RoutingTest
public class GeneratorServiceTest {

  private final DbManager dbManager;
  private final Http1Client client;

  public GeneratorServiceTest(Http1Client client){
    this.client = client;
    dbManager = Contexts.globalContext().get(DbManager.class).orElseThrow();
  }

  @BeforeAll
  static void beforeAll() throws IOException, InterruptedException {
    DbUtils.resetDb();
    DbUtils.initializeLLM();
    OllamaSetupManager.setupAndStartOllama();
  }

  @AfterAll
  static void afterAll() throws IOException, InterruptedException {
    OllamaSetupManager.stopOllama();
  }

  @SetUpRoute
  static void routing(HttpRouting.Builder builder) throws IOException {
    DbManager.initialize();
    builder.register("/", new GeneratorService());
  }

  @Test
  void testReceivePrompt() {
    var chat = new Chat(0, "Title", Timestamp.valueOf("2024-10-31 22:50:25"), 1);
    dbManager.insertChat(chat);
    var chatId = dbManager.getChatByParams(chat).id();
    var prompt = new Prompt(1, "Sample message", AuthorType.USER, chatId, true);
    try (var response = client.post("/class").submit(prompt)) {
      int promptId = response.as(Integer.class);
      assertAll(
              () -> assertEquals(Status.OK_200, response.status()),
              () -> assertEquals(0, PLAINTEXT_UTF_8.compareTo(response.headers().contentType().orElseThrow()))
      );
      dbManager.deletePromptById(promptId);
    }
    dbManager.deleteChatById(chatId);
  }

  @Test
  void testStreamLLMResponse() {
    var chat = new Chat(0, "Title", Timestamp.valueOf("2024-10-31 22:50:25"), 1);
    dbManager.insertChat(chat);
    var chatId = dbManager.getChatByParams(chat).id();
    Prompt prompt =
            new Prompt(1, "Generate a Java class that computes big prime numbers.", AuthorType.USER, chatId, true);
    int registeredPromptId;
    try (var response = client.post("/class").submit(prompt)) {
      registeredPromptId = response.as(Integer.class);
      assertAll(
              () -> assertEquals(Status.OK_200, response.status()),
              () -> assertEquals(0, PLAINTEXT_UTF_8.compareTo(response.headers().contentType().orElseThrow()))
      );
    }
    try (var response = client.get("/stream/" + registeredPromptId).header(ACCEPT_EVENT_STREAM).request()) {
      var latch = new CountDownLatch(1);
      response.source(SseSource.TYPE, _ -> latch.countDown());

      assertAll(
              () -> assertEquals(Status.OK_200, response.status()),
              () -> assertEquals(TEXT_EVENT_STREAM, response.headers().contentType().orElseThrow().mediaType())
      );
    }
    dbManager.deletePromptById(registeredPromptId);
    dbManager.deleteChatById(chatId);
  }
}
