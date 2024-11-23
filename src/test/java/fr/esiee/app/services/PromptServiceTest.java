package fr.esiee.app.services;

import fr.esiee.app.db.DbManager;
import fr.esiee.app.db.entities.AuthorType;
import fr.esiee.app.db.entities.Chat;
import fr.esiee.app.db.entities.Prompt;
import fr.esiee.tests.DbUtils;
import io.helidon.common.context.Contexts;
import io.helidon.http.NotFoundException;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.function.BiPredicate;

import static io.helidon.http.HttpMediaTypes.JSON_PREDICATE;
import static io.helidon.http.HttpMediaTypes.PLAINTEXT_UTF_8;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RoutingTest
public class PromptServiceTest {
  private final DbManager dbManager;
  private final Http1Client client;

  private final BiPredicate<Prompt, Prompt> promptPredicate =
          (p1, p2) -> p1.message().equals(p2.message()) && p1.authorType() == p2.authorType() &&
                  p1.chatId() == p2.chatId() && p1.compile() == p2.compile();

  @BeforeAll
  static void beforeAll() throws IOException {
    DbUtils.resetDb();
    DbUtils.initializeRealLLM();
  }

  public PromptServiceTest(Http1Client client) throws IOException {
    this.client = client;
    dbManager = Contexts.globalContext().get(DbManager.class).orElseThrow();
  }

  @SetUpRoute
  static void routing(HttpRouting.Builder builder) throws IOException {
    DbManager.initialize();
    builder.register("/", new PromptService());
  }

  List<Chat> getChats() {
    var chats = dbManager.listChats();
    if (chats.isEmpty()) {
      var chat = new Chat(0, "Title", Timestamp.valueOf("2024-10-31 22:50:25"), 1);
      var chat2 = new Chat(0, "Title2", Timestamp.valueOf("2024-10-31 22:50:25"), 2);
      dbManager.insertChat(chat);
      dbManager.insertChat(chat2);
      chats = dbManager.listChats();
    }
    return chats;
  }

  @Test
  void testListPrompts() {
    var chats = getChats();
    var prompt1 = new Prompt(0, "Prompt1", AuthorType.USER, chats.stream().findAny().orElseThrow().id(), false);
    var prompt2 = new Prompt(0, "Prompt2", AuthorType.USER, chats.stream().findAny().orElseThrow().id(), false);
    dbManager.insertPrompt(prompt1);
    dbManager.insertPrompt(prompt2);
    try (var response = client.get("/").request()) {
      var prompts = response.as(Prompt[].class);
      var promptsFromDb = dbManager.listPrompts();
      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertTrue(JSON_PREDICATE.test(response.headers().contentType().orElseThrow())),
              () -> assertEquals(prompts.length, promptsFromDb.size()),
              () -> assertEquals(List.of(prompts), promptsFromDb));
    }
  }

  @Test
  void testInsertPrompt() {
    var chats = getChats();
    var prompt = new Prompt(0, "Prompt1", AuthorType.USER, chats.stream().findAny().orElseThrow().id(), false);
    try (var response = client.post("/").submit(prompt)) {
      var promptFromDb = dbManager.listPrompts().stream().filter(p -> promptPredicate.test(p, prompt)).findAny();
      assertAll(() -> assertEquals(Status.CREATED_201, response.status()),
              () -> assertEquals(0, PLAINTEXT_UTF_8.compareTo(response.headers().contentType().orElseThrow())),
              () -> assertDoesNotThrow(() -> promptFromDb.orElseThrow()));
    }
  }

  @Test
  void testUpdatePrompt() {
    var chats = getChats();
    var prompt = new Prompt(0, "Prompt1", AuthorType.USER, chats.stream().findAny().orElseThrow().id(), false);
    dbManager.insertPrompt(prompt);
    var promptFromDb =
            dbManager.listPrompts().stream().filter(p -> promptPredicate.test(p, prompt)).findAny().orElseThrow();
    var promptUpdated = new Prompt(promptFromDb.id(), "PromptUpdated", AuthorType.AI, promptFromDb.chatId(),
            !promptFromDb.compile());
    try (var response = client.put("/").submit(promptUpdated)) {
      var promptFromDbUpdated = dbManager.getPromptById(promptFromDb.id());
      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertEquals(0, PLAINTEXT_UTF_8.compareTo(response.headers().contentType().orElseThrow())),
              () -> assertEquals(promptUpdated, promptFromDbUpdated));
    }
  }

  @Test
  void testDeletePromptById() {
    var chats = getChats();
    var prompt = new Prompt(0, "Prompt1", AuthorType.USER, chats.stream().findAny().orElseThrow().id(), false);
    dbManager.insertPrompt(prompt);
    var promptFromDb =
            dbManager.listPrompts().stream().filter(p -> promptPredicate.test(p, prompt)).findAny().orElseThrow();
    try (var response = client.delete("/" + promptFromDb.id()).request()) {
      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertEquals(0, PLAINTEXT_UTF_8.compareTo(response.headers().contentType().orElseThrow())),
              () -> assertThrows(NotFoundException.class, () -> dbManager.getPromptById(promptFromDb.id())));
    }
  }

  @Test
  void testGetPromptById() {
    var chats = getChats();
    var prompt = new Prompt(0, "Prompt1", AuthorType.USER, chats.stream().findAny().orElseThrow().id(), false);
    dbManager.insertPrompt(prompt);
    var promptFromDb =
            dbManager.listPrompts().stream().filter(p -> promptPredicate.test(p, prompt)).findAny().orElseThrow();
    try (var response = client.get("/" + promptFromDb.id()).request()) {
      var promptFromResponse = response.as(Prompt.class);
      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertTrue(JSON_PREDICATE.test(response.headers().contentType().orElseThrow())),
              () -> assertEquals(promptFromDb, promptFromResponse));
    }
  }

  @Test
  void testGetPromptsByChatId() {
    var chats = getChats();
    var prompt1 = new Prompt(0, "Prompt1", AuthorType.USER, chats.stream().findAny().orElseThrow().id(), false);
    var prompt2 = new Prompt(0, "Prompt2", AuthorType.USER, chats.stream().findAny().orElseThrow().id(), false);
    dbManager.insertPrompt(prompt1);
    dbManager.insertPrompt(prompt2);
    try (var response = client.get("/bychat/" + prompt1.chatId()).request()) {
      var prompts = response.as(Prompt[].class);
      var promptsFromDb = dbManager.getPromptsByChatId(prompt1.chatId());
      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertTrue(JSON_PREDICATE.test(response.headers().contentType().orElseThrow())),
              () -> assertEquals(prompts.length, promptsFromDb.size()),
              () -> assertEquals(List.of(prompts), promptsFromDb));
    }
  }

  @Test
  void testGetFirstPromptByChatId() {
    var chats = getChats();
    var prompt1 = new Prompt(0, "Prompt1", AuthorType.USER, chats.stream().findAny().orElseThrow().id(), false);
    var prompt2 = new Prompt(0, "Prompt2", AuthorType.USER, chats.stream().findAny().orElseThrow().id(), false);
    dbManager.insertPrompt(prompt1);
    dbManager.insertPrompt(prompt2);
    try (var response = client.get("/bychat/" + prompt1.chatId() + "/first").request()) {
      var prompt = response.as(Prompt.class);
      var promptFromDb = dbManager.getFirstPromptByChatId(prompt1.chatId());
      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertTrue(JSON_PREDICATE.test(response.headers().contentType().orElseThrow())),
              () -> assertEquals(prompt, promptFromDb));
    }
  }
}
