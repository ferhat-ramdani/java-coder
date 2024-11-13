package fr.esiee.app.services;

import fr.esiee.app.db.DbManager;
import fr.esiee.app.db.entities.Chat;
import io.helidon.common.context.Contexts;
import io.helidon.http.NotFoundException;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

import static io.helidon.http.HttpMediaTypes.JSON_UTF_8;
import static io.helidon.http.HttpMediaTypes.PLAINTEXT_UTF_8;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RoutingTest
public class ChatServiceTest {

  private final DbManager dbManager;
  private final Http1Client client;

  public ChatServiceTest(Http1Client client) {
    this.client = client;
    dbManager = Contexts.globalContext().get(DbManager.class).orElseThrow();
  }

  @SetUpRoute
  static void routing(HttpRouting.Builder builder) throws IOException {
    DbManager.initialize();
    builder.register("/", new ChatService());
  }

  @Test
  void testInsertChat() {
    var chat = new Chat(0, "Title", Timestamp.valueOf("2024-10-31 22:50:25"), 1);
    try (var response = client.post("/").submit(chat)) {
      var chatFromResponse = response.as(Chat.class);
      var chatFromDb = dbManager.getChatById(chatFromResponse.id());
      assertAll(() -> assertEquals(Status.CREATED_201, response.status()),
              () -> assertEquals(0, JSON_UTF_8.compareTo(response.headers().contentType().orElseThrow())),
              () -> assertEquals(chat.title(), chatFromDb.title()),
              () -> assertEquals(chat.lastActivity(), chatFromDb.lastActivity()),
              () -> assertEquals(chat.llmId(), chatFromDb.llmId()));
    }
  }

  @Test
  void testGetListOfChat() {
    var chat1 = new Chat(0, "Title", Timestamp.valueOf("2024-10-31 22:50:25"), 1);
    var chat2 = new Chat(0, "Title2", Timestamp.valueOf("2024-10-31 22:50:25"), 2);
    var chat3 = new Chat(0, "Title3", Timestamp.valueOf("2024-10-31 22:50:25"), 3);
    dbManager.insertChat(chat1);
    dbManager.insertChat(chat2);
    dbManager.insertChat(chat3);

    try (var response = client.get("/").request()) {
      var chats = response.as(Chat[].class);
      var chatsFromDb = dbManager.listChats();
      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertEquals(0, JSON_UTF_8.compareTo(response.headers().contentType().orElseThrow())),
              () -> assertEquals(chats.length, chatsFromDb.size()),
              () -> assertEquals(List.of(chats), chatsFromDb));
    }
  }

  @Test
  void testGetChatById() {
    var chat1 = new Chat(0, "Title", Timestamp.valueOf("2024-10-31 22:50:25"), 1);
    dbManager.insertChat(chat1);
    var chatFromDb = dbManager.getChatByParams(chat1);
    try (var response = client.get("/" + chatFromDb.id()).request()) {
      var chat = response.as(Chat.class);
      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertEquals(0, JSON_UTF_8.compareTo(response.headers().contentType().orElseThrow())),
              () -> assertEquals(chat, chatFromDb));
    }
  }

  @Test
  void testUpdateChat() {
    var chat1 = new Chat(0, "Title", Timestamp.valueOf("2024-10-31 22:50:25"), 1);
    dbManager.insertChat(chat1);
    var chatFromDb1 = dbManager.getChatByParams(chat1);
    var chat = new Chat(chatFromDb1.id(), "TitleUpdated", Timestamp.valueOf("2024-10-31 22:50:25"), 1);
    try (var response = client.put("/").submit(chat)) {
      var chatFromDb = dbManager.getChatById(chat.id());
      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertEquals(0, PLAINTEXT_UTF_8.compareTo(response.headers().contentType().orElseThrow())),
              () -> assertEquals(chat, chatFromDb));
    }
  }

  @Test
  void testDeleteChat() {
    var chat1 = new Chat(0, "Title", Timestamp.valueOf("2024-10-31 22:50:25"), 1);
    dbManager.insertChat(chat1);
    var chatFromDb = dbManager.getChatByParams(chat1);
    try (var response = client.delete("/" + chatFromDb.id()).request()) {
      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertThrows(NotFoundException.class, () -> dbManager.getChatById(chatFromDb.id())),
              () -> assertEquals(0, PLAINTEXT_UTF_8.compareTo(response.headers().contentType().orElseThrow())));
    }
  }
}

