package fr.esiee.app.db;

import fr.esiee.app.db.entities.AuthorType;
import fr.esiee.app.db.entities.Chat;
import fr.esiee.app.db.entities.LLM;
import fr.esiee.app.db.entities.Prompt;
import fr.esiee.tests.DbUtils;
import io.helidon.common.context.Contexts;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.http.NotFoundException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbManagerTest {

  private DbClient dbClient;
  private DbManager dbManager;

  @BeforeAll
  static void cleanDB() {
    DbUtils.resetDb();
  }

  @AfterAll
  static void restoreDB() throws IOException {
    var config = Config.global().get("db");
    try(var dbClient = DbClient.builder(config).build()) {
      var dbManager = new DbManager(dbClient);
      dbManager.setupSchema();
      dbManager.setupData();
    }
  }

  @BeforeEach
  void initializeDBManger() {
    var config = Config.global().get("db");
    dbClient = DbClient.builder(config).build();
    dbManager = new DbManager(dbClient);
    dbManager.setupSchema();
  }

  @AfterEach
  void closeDBManager() {
    dbClient.close();
    DbUtils.resetDb();
  }

  @Test
  void testSetupSchema() {
    var tableName = dbClient.execute().createQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES;").execute()
            .map(e -> e.column("TABLE_NAME").getString()).toList();
    assertAll(() -> assertTrue(tableName.contains("LLM")), () -> assertTrue(tableName.contains("CHAT")),
            () -> assertTrue(tableName.contains("PROMPT")));
  }

  @Test
  void testSetupData() throws IOException {
    dbManager.setupData();
    var llms = dbClient.execute().query("SELECT * FROM llm").map(e -> e.as(LLM.class)).toList();
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
  void testListChats() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    var chats = dbManager.listChats();
    assertEquals(3, chats.size());
  }

  @Test
  void testListPrompts() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    DbUtils.initializePrompts();
    var prompts = dbManager.listPrompts();
    assertEquals(18, prompts.size());
  }

  @Test
  void testGetPromptByIdExists() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    DbUtils.initializePrompts();
    var prompt = dbManager.getPromptById(2);
    assertEquals(DbUtils.prompts().get(1), prompt);
  }

  @Test
  void testGetPromptByIdNotExists() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    DbUtils.initializePrompts();
    assertThrows(NotFoundException.class, () -> dbManager.getPromptById(100));
  }


  @Test
  void testInsertPrompt() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    var prompt = new Prompt(1, "Test", AuthorType.SYSTEM, 1, false);
    dbManager.insertPrompt(prompt);
    var prompts = dbManager.listPrompts();
    assertAll(() -> assertEquals(1, prompts.size()), () -> assertEquals(prompt, prompts.getFirst()));
  }

  @Test
  void testUpdateChatLastActivity() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    var chat = DbUtils.chats().getFirst();
    dbManager.updateChatLastActivity(chat.id());
    var updatedChat = dbManager.listChats().getFirst();
    assertTrue(updatedChat.lastActivity().getTime() > chat.lastActivity().getTime());
  }

  @Test
  void testUpdatePrompt() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    DbUtils.initializePrompts();
    var prompt = DbUtils.prompts().getFirst();
    var newPrompt = new Prompt(prompt.id(), "Test", AuthorType.USER, 1, false);
    dbManager.updatePrompt(newPrompt);
    var updatedPrompt = dbManager.listPrompts().getFirst();

    assertEquals(newPrompt, updatedPrompt);
  }

  @Test
  void testDeletePromptById() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    DbUtils.initializePrompts();
    var prompt = DbUtils.prompts().getFirst();
    dbManager.deletePromptById(prompt.id());
    var prompts = dbManager.listPrompts();

    assertAll(() -> assertEquals(17, prompts.size()), () -> assertNotEquals(prompt, prompts.getFirst()));
  }

  @Test
  void testGetPromptsByChatId() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    DbUtils.initializePrompts();
    var chatId = 1;
    var prompts = dbManager.getPromptsByChatId(chatId);
    assertEquals(6, prompts.size());
  }

  @Test
  void testInsertChat() throws IOException {
    DbUtils.initializeLLM();
    var chat = new Chat(1, "Test", Timestamp.valueOf("2024-10-31 22:50:25"), 1);
    dbManager.insertChat(chat);
    var chats = dbManager.listChats();

    assertAll(() -> assertEquals(1, chats.size()), () -> assertEquals(chat, chats.getFirst()));
  }

  @Test
  void testGetChatByParams() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    var chat = DbUtils.chats().getFirst();
    var chatParams = dbManager.getChatByParams(chat);
    assertEquals(chat, chatParams);
  }

  @Test
  void testGetPromptByPromptInfo() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    DbUtils.initializePrompts();
    var prompt = DbUtils.prompts().getFirst();
    var promptInfo = dbManager.getPromptByPromptInfo(prompt);
    assertEquals(prompt, promptInfo);
  }

  @Test
  void testChatExists() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    var chat = DbUtils.chats().getFirst();
    assertTrue(dbManager.chatExists(chat.id()));
  }

  @Test
  void testChatNotExists() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    var chat = new Chat(100, "Test", Timestamp.from(Instant.now()), 1);
    assertFalse(dbManager.chatExists(chat.id()));
  }

  @Test
  void testPromptExists() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    DbUtils.initializePrompts();
    var prompt = DbUtils.prompts().getLast();
    assertTrue(dbManager.promptExists(prompt.id()));
  }

  @Test
  void testPromptNotExists() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    DbUtils.initializePrompts();
    var prompt = new Prompt(100, "Test", AuthorType.USER, 1, false);
    assertFalse(dbManager.promptExists(prompt.id()));
  }

  @Test
  void testGetChatById() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    var chat = DbUtils.chats().getLast();
    var chatById = dbManager.getChatById(chat.id());
    assertEquals(chat, chatById);
  }

  @Test
  void testUpdateChat() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    var chat = DbUtils.chats().getLast();
    var newChat = new Chat(chat.id(), "Test", Timestamp.valueOf("2024-12-24 12:32:59"), 1);
    dbManager.updateChat(newChat);
    var updatedChat = dbManager.getChatById(chat.id());

    assertEquals(newChat, updatedChat);
  }

  @Test
  void testDeleteChatById() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    var chat = DbUtils.chats().getLast();
    dbManager.deleteChatById(chat.id());
    var chatsDb = dbManager.listChats();

    assertAll(() -> assertEquals(2, chatsDb.size()), () -> assertNotEquals(chat, chatsDb.getLast()));

  }

  @Test
  void testListLLMs() throws IOException {
    DbUtils.initializeLLM();
    var llms = dbManager.listLLMs();
    assertEquals(2, llms.size());
  }

  @Test
  void testGetFirstLLM() throws IOException {
    DbUtils.initializeLLM();
    var llm = DbUtils.llms().getFirst();
    var firstLLM = dbManager.getFirstLLM();
    assertEquals(llm, firstLLM);
  }

  @Test
  void testGetLLMByIdExists() throws IOException {
    DbUtils.initializeLLM();
    var llm = DbUtils.llms().getFirst();
    var llmById = dbManager.getLLMById(llm.id());
    assertEquals(llm, llmById);
  }

  @Test
  void testGetLLMByIdNotExists() throws IOException {
    DbUtils.initializeLLM();
    assertThrows(NotFoundException.class, () -> dbManager.getLLMById(100));
  }

  @Test
  void testGetFirstPromptByChatId() throws IOException {
    DbUtils.initializeLLM();
    DbUtils.initializeChats();
    DbUtils.initializePrompts();

    var chatId = 1;
    var prompt = dbManager.getFirstPromptByChatId(chatId);
    var prompts = DbUtils.prompts();

    assertAll(() -> assertEquals(prompts.getFirst(), prompt), () -> assertNotEquals(prompts.getLast(), prompt));
  }

  @Test
  void testTruncate() {
    var bigString = "a".repeat(250);
    var normalizedString = "a".repeat(100);

    var result = DbManager.truncate(bigString, 100);
    assertAll(() -> assertEquals(100, result.length()), () -> assertEquals(normalizedString, result),
            () -> assertEquals(result, DbManager.truncate(result, 150)));
  }


  @Test
  void testBugTitileChat() throws IOException {
    DbUtils.initializeLLM();
    var title = "MgFxgoN1xkZHMCzuFAdDtF9wOyoxrze2v4veXkQsd0BgorAyzt8SWn0s6BTa52MvYsRspPF0tYGBy985FKp2FaRJVDHdtTjChVW4MgFxgoN1xkZHMCzuFAdDtF9wOyoxrze2v4veXkQsd0BgorAyzt8SWn0s6BTa52MvYsRspPF0tYGBy985FKp2FaRJVDHdtTjChVW4";
    var chat = new Chat(1, title, Timestamp.valueOf("2024-10-31 22:50:25"), 1);
    var titleTruncate = DbManager.truncate(title, 100);
    dbManager.insertChat(chat);
    var chats = dbManager.listChats();
    assertAll(() -> assertEquals(1, chats.size()),
            () -> assertEquals(titleTruncate, chats.getFirst().title()));
  }
}
