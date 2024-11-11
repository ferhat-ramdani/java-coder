package fr.esiee.app.services;

import fr.esiee.app.db.DbManager;
import fr.esiee.app.db.entities.AuthorType;
import fr.esiee.app.db.entities.Chat;
import fr.esiee.app.db.entities.Prompt;
import io.helidon.common.context.Contexts;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Timestamp;

import static io.helidon.http.HttpMediaTypes.*;
import static org.junit.jupiter.api.Assertions.*;

@RoutingTest
public class GeneratorServiceTest {

  private final DbManager dbManager;
  private final Http1Client client;

  public GeneratorServiceTest(Http1Client client) {
    this.client = client;
    dbManager = Contexts.globalContext().get(DbManager.class).orElseThrow();
  }

  @SetUpRoute
  static void routing(HttpRouting.Builder builder) throws IOException {
    DbManager.initialize();
    builder.register("/api/gen", new GeneratorService());
  }

  @Test
  void testReceivePrompt() {
    var chat = new Chat(0, "Title", Timestamp.valueOf("2024-10-31 22:50:25"), 1);
    dbManager.insertChat(chat);
    var chatId = dbManager.getChatByParams(chat).id();
    Prompt prompt = new Prompt(1, "Sample message", AuthorType.USER, chatId, true);
    dbManager.insertPrompt(prompt);
    try(var response = client.post("/api/gen/class").submit(prompt)) {
      var responseText = response.as(String.class);
      assertAll(
              () -> assertEquals(Status.OK_200, response.status()),
              () -> assertEquals(0, PLAINTEXT_UTF_8.compareTo(response.headers().contentType().orElseThrow())),
              () -> assertEquals("Prompt received successfully.", responseText)
      );
    }
  }

  @Test
  void testStreamLLMResponse() {
    var chat = new Chat(0, "Title", Timestamp.valueOf("2024-10-31 22:50:25"), 1);
    dbManager.insertChat(chat);
    var chatId = dbManager.getChatByParams(chat).id();
    Prompt prompt = new Prompt(1, "Generate a Java class that computes big prime numbers.", AuthorType.USER, chatId, true);
    dbManager.insertPrompt(prompt);
    try (var response = client.post("/api/gen/class").submit(prompt)) {
      var responseText = response.as(String.class);
      assertAll(
              () -> assertEquals(Status.OK_200, response.status()),
              () -> assertEquals(0, PLAINTEXT_UTF_8.compareTo(response.headers().contentType().orElseThrow())),
              () -> assertEquals("Prompt received successfully.", responseText)
      );
    }
    try (var response = client.get("/api/gen/stream").request()) {
      assertAll(
              () -> assertEquals(Status.OK_200, response.status()),
              () -> assertEquals("text/event-stream", response.headers().contentType().orElseThrow().text())
      );
    }
  }

  @Test
  void testExecuteClass() {
    var chat = new Chat(0, "Title", Timestamp.valueOf("2024-10-31 22:50:25"), 1);
    dbManager.insertChat(chat);
    var chatId = dbManager.getChatByParams(chat).id();
    Prompt prompt = new Prompt(1, "public class PrimeCalculator { public static void main(String[] args) { for (int i = 2; i < 100; i++) { if (isPrime(i)) { System.out.println(i); } } } public static boolean isPrime(int num) { if (num <= 1) return false; for (int i = 2; i <= Math.sqrt(num); i++) { if (num % i == 0) return false; } return true; } }", AuthorType.AI, chatId, true);
    dbManager.insertPrompt(prompt);
    try (var response = client.post("/api/gen/exec").submit(1)) {
      assertAll(
              () -> assertEquals(Status.OK_200, response.status()),
              () -> assertEquals(0, PLAINTEXT_UTF_8.compareTo(response.headers().contentType().orElseThrow())),
              () -> assertNotNull(response.as(String.class))
      );
    }
  }
}
