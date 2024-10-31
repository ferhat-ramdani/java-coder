package fr.esiee.app.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.esiee.app.db.entities.AuthorType;
import fr.esiee.app.db.entities.Chat;
import fr.esiee.app.db.entities.LLM;
import fr.esiee.app.db.entities.Prompt;
import io.helidon.common.context.Contexts;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.http.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbManagerTest {


  @Mock
  private DbClient dbClient;

  private DbManager dbManager;

  private List<Chat> chats = List.of(new ObjectMapper().readValue(DbManagerTest.class.getResourceAsStream("/chats.json"), Chat[].class));
  private List<Prompt> prompts =
          List.of(new ObjectMapper().readValue(DbManagerTest.class.getResourceAsStream("/prompts.json"), Prompt[].class));

  DbManagerTest() throws IOException {
  }

  @BeforeEach
  void initializeDBManger() throws Exception {
    var config = Config.global().get("db");
    dbClient = DbClient.builder(config).build();
    dbManager = new DbManager(dbClient);
    dbManager.setupSchema();
    dbManager.setupData();
  }

  private void initializeChat() {
    for (var chat : chats) {
      dbClient.execute().namedInsert("insert-chat",
              chat.title(),
              chat.lastActivity(),
              chat.llmId()
      );
    }
  }

  private void initializePrompt() {
    for (var prompt : prompts) {
      dbClient.execute().namedInsert("insert-prompt",
              prompt.message(),
              prompt.authorType().name(),
              prompt.chatId(),
              prompt.compile()
      );
    }
  }

  @AfterEach
  void closeDBManager() {
    dbClient.execute().namedDelete("delete-all-llms");
    dbClient.execute().namedDelete("delete-all-chats");
    dbClient.execute().namedDelete("delete-all-prompts");
    dbClient.close();
  }

  @Test
  void testSetupSchema() {
    var tableName = dbClient.execute().createQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES;").execute()
            .map(e -> e.column("TABLE_NAME").getString()).toList();
    assertTrue(tableName.contains("CHAT"));
    assertTrue(tableName.contains("LLM"));
    assertTrue(tableName.contains("PROMPT"));
  }

  @Test
  void testSetupData() {
    var llms = dbClient.execute().namedQuery("select-all-llms").map(e -> e.as(LLM.class)).toList();

    assertEquals(3, llms.size());
  }

  @Test
  void testInitialize() throws IOException {
    DbManager.initialize();
    assertDoesNotThrow(() -> {
      Contexts.globalContext().get(DbManager.class).orElseThrow();
    });
  }


  @Test
  void testListChats() {
    initializeChat();
    var chats = dbManager.listChats();
    assertEquals(3, chats.size());
  }

  @Test
  void testListPrompts() {
    initializeChat();
    initializePrompt();
    var prompts = dbManager.listPrompts();
    assertEquals(18, prompts.size());
  }

  @Test
  void testGetPromptByIdExists() {
    initializeChat();
    initializePrompt();
    var prompt = dbManager.getPromptById(2);
    assertEquals(prompts.get(1), prompt);
  }

  @Test
  void testGetPromptByIdNotExists() {
    initializeChat();
    initializePrompt();
    assertThrows(NotFoundException.class, () -> dbManager.getPromptById(100));
  }


  @Test
  void testInsertPrompt() {
    initializeChat();
    var prompt = new Prompt(1, "Test", AuthorType.SYSTEM, 1, false);
    dbManager.insertPrompt(prompt);
    var prompts = dbManager.listPrompts();
    assertEquals(1, prompts.size());
    assertEquals(prompt, prompts.getFirst());
  }

  @Test
  void testUpdateChatLastActivity() {
    initializeChat();
    var chat = dbManager.listChats().getFirst();
    var newTime = Timestamp.from(Instant.now());
    dbManager.updateChatLastActivity(chat.id());
    var updatedChat = dbManager.listChats().getFirst();
    assertEquals(newTime.getTime(), updatedChat.lastActivity().getTime());
  }

  @Test
  void testUpdatePrompt() {
    initializeChat();
    initializePrompt();
    var prompt = dbManager.listPrompts().getFirst();
    var newPrompt = new Prompt(prompt.id(), "Test", AuthorType.USER, 1, false);
    dbManager.updatePrompt(newPrompt);
    var updatedPrompt = dbManager.listPrompts().getFirst();
    assertEquals(newPrompt, updatedPrompt);
    assertNotEquals(prompt, updatedPrompt);
  }









  //--------------------
  @Test
  void testInsertChat() {
    var chat = new Chat(1, "Test", Timestamp.valueOf("2024-10-31 22:50:25"), 1);
    dbManager.insertChat(chat);
    var chats = dbManager.listChats();
    assertEquals(1, chats.size());
    assertEquals(chat, chats.getFirst());
  }


}
