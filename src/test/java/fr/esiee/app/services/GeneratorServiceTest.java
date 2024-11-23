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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.concurrent.CountDownLatch;

import static io.helidon.common.media.type.MediaTypes.TEXT_EVENT_STREAM;
import static io.helidon.http.HeaderValues.ACCEPT_EVENT_STREAM;
import static io.helidon.http.HttpMediaTypes.PLAINTEXT_UTF_8;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@RoutingTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class GeneratorServiceTest {

  private final DbManager dbManager;
  private final Http1Client client;

  public GeneratorServiceTest(Http1Client client) throws IOException, InterruptedException {
    DbUtils.resetDb();
    DbUtils.initializeLLM();
    this.client = client;
    dbManager = Contexts.globalContext().get(DbManager.class).orElseThrow();
    OllamaSetupManager.setupAndStartOllama();
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
    int registeredPromptId = 1;
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

  @Test
  void testExecuteClass() {
    var chat = new Chat(0, "Title", Timestamp.valueOf("2024-10-31 22:50:25"), 1);
    dbManager.insertChat(chat);
    var chatId = dbManager.getChatByParams(chat).id();
    Prompt prompt = new Prompt(1,
            "public class PrimeCalculator { public static void main(String[] args) { for (int i = 2; i < 100; i++) { if (isPrime(i)) { System.out.println(i); } } } public static boolean isPrime(int num) { if (num <= 1) return false; for (int i = 2; i <= Math.sqrt(num); i++) { if (num % i == 0) return false; } return true; } }",
            AuthorType.AI, chatId, true);
    dbManager.insertPrompt(prompt);
    var promptId = dbManager.getPromptByPromptInfo(prompt).id();
    try (var response = client.post("/exec").submit(promptId)) {
      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertEquals(0, PLAINTEXT_UTF_8.compareTo(response.headers().contentType().orElseThrow())),
              () -> assertNotNull(response.as(String.class)));
    }
    dbManager.deletePromptById(promptId);
    dbManager.deleteChatById(chatId);
  }
}
