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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbManagerTest {

  private DbClient dbClient;

  private DbManager dbManager;

  private List<LLM> llms = List.of(new ObjectMapper().readValue(DbManagerTest.class.getResourceAsStream("/llms_test.json"), LLM[].class));
  private List<Chat> chats = List.of(new ObjectMapper().readValue(DbManagerTest.class.getResourceAsStream("/chats.json"), Chat[].class));
  private List<Prompt> prompts =
          List.of(new ObjectMapper().readValue(DbManagerTest.class.getResourceAsStream("/prompts.json"), Prompt[].class));

  DbManagerTest() throws IOException {
  }

  @BeforeEach
  void initializeDBManger() {
    var config = Config.global().get("db");
    dbClient = DbClient.builder(config).build();
    dbManager = new DbManager(dbClient);
    dbManager.setupSchema();
  }

  private void initializeLLM() {
    for (var llm : llms) {
      dbClient.execute().createInsert("INSERT INTO llm(id, name, model, system_prompt, characteristics, temp, seed) VALUES(?, ?, ?, ?, ?, ?, ?)")
              .addParam(llm.id())
              .addParam(llm.name())
              .addParam(llm.model())
              .addParam(llm.systemPrompt())
              .addParam(llm.characteristics())
              .addParam(llm.temp())
              .addParam(llm.seed())
              .execute();
    }
  }

  private void initializeChat() {
    for (var chat : chats) {
      dbClient.execute().createInsert("INSERT INTO Chat(id, title, last_activity, llm_id) VALUES(?, ?, ?, ?)")
              .addParam(chat.id())
              .addParam(chat.title())
              .addParam(chat.lastActivity())
              .addParam(chat.llmId())
              .execute();
    }
  }

  private void initializePrompt() {
    for (var prompt : prompts) {
      dbClient.execute().createInsert("INSERT INTO Prompt(id, message, author_type, chat_id, compile) VALUES(?, ?, ?, ?, ?)")
              .addParam(prompt.id())
              .addParam(prompt.message())
              .addParam(prompt.authorType().name())
              .addParam(prompt.chatId())
              .addParam(prompt.compile())
              .execute();
    }
  }

  @AfterEach
  void closeDBManager() {
    dbClient.execute().createDelete("DELETE FROM llm").execute();
    dbClient.execute().createDelete("DELETE FROM chat").execute();
    dbClient.execute().createDelete("DELETE FROM prompt").execute();
    dbClient.close();
  }

  @Test
  void testSetupSchema() {
    var tableName = dbClient.execute().createQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES;").execute()
            .map(e -> e.column("TABLE_NAME").getString()).toList();
    assertAll(
            () -> assertTrue(tableName.contains("LLM")),
            () -> assertTrue(tableName.contains("CHAT")),
            () -> assertTrue(tableName.contains("PROMPT"))
    );
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
  void testListChats() {
    initializeLLM();
    initializeChat();
    var chats = dbManager.listChats();
    assertEquals(3, chats.size());
  }

  @Test
  void testListPrompts() {
    initializeLLM();
    initializeChat();
    initializePrompt();
    var prompts = dbManager.listPrompts();
    assertEquals(18, prompts.size());
  }

  @Test
  void testGetPromptByIdExists() {
    initializeLLM();
    initializeChat();
    initializePrompt();
    var prompt = dbManager.getPromptById(2);
    assertEquals(prompts.get(1), prompt);
  }

  @Test
  void testGetPromptByIdNotExists() {
    initializeLLM();
    initializeChat();
    initializePrompt();
    assertThrows(NotFoundException.class, () -> dbManager.getPromptById(100));
  }


  @Test
  void testInsertPrompt() {
    initializeLLM();
    initializeChat();
    var prompt = new Prompt(1, "Test", AuthorType.SYSTEM, 1, false);
    dbManager.insertPrompt(prompt);
    var prompts = dbManager.listPrompts();
    assertEquals(1, prompts.size());
    assertEquals(prompt, prompts.getFirst());
  }

  @Test
  void testUpdateChatLastActivity() {
    initializeLLM();
    initializeChat();
    var chat = chats.getFirst();
    dbManager.updateChatLastActivity(chat.id());
    var updatedChat = dbManager.listChats().getFirst();
    assertTrue(updatedChat.lastActivity().getTime() > chat.lastActivity().getTime());
  }

  @Test
  void testUpdatePrompt() {
    initializeLLM();
    initializeChat();
    initializePrompt();
    var prompt = prompts.getFirst();
    var newPrompt = new Prompt(prompt.id(), "Test", AuthorType.USER, 1, false);
    dbManager.updatePrompt(newPrompt);
    var updatedPrompt = dbManager.listPrompts().getFirst();
    assertEquals(newPrompt, updatedPrompt);
    assertNotEquals(prompt, updatedPrompt);
  }

  @Test
  void testDeletePromptById() {
    initializeLLM();
    initializeChat();
    initializePrompt();
    var prompt = prompts.getFirst();
    dbManager.deletePromptById(prompt.id());
    var prompts = dbManager.listPrompts();
    assertEquals(17, prompts.size());
    assertNotEquals(prompt, prompts.getFirst());
  }

  @Test
  void testGetPromptsByChatId() {
    initializeLLM();
    initializeChat();
    initializePrompt();
    var chatId = 1;
    var prompts = dbManager.getPromptsByChatId(chatId);
    assertEquals(6, prompts.size());
  }

  @Test
  void testInsertChat() {
    initializeLLM();
    var chat = new Chat(1, "Test", Timestamp.valueOf("2024-10-31 22:50:25"), 1);
    dbManager.insertChat(chat);
    var chats = dbManager.listChats();
    assertEquals(1, chats.size());
    assertEquals(chat, chats.getFirst());
  }

  @Test
  void testGetChatByParams() {
    initializeLLM();
    initializeChat();
    var chat = chats.getFirst();
    var chatParams = dbManager.getChatByParams(chat);
    assertEquals(chat, chatParams);
  }

  @Test
  void testGetPromptByPromptInfo() {
    initializeLLM();
    initializeChat();
    initializePrompt();
    var prompt = prompts.getFirst();
    var promptInfo = dbManager.getPromptByPromptInfo(prompt);
    assertEquals(prompt, promptInfo);
  }

  @Test
  void testChatExists(){
    initializeLLM();
    initializeChat();
    var chat = chats.getFirst();
    assertTrue(dbManager.chatExists(chat.id()));
  }

  @Test
  void testChatNotExists(){
    initializeLLM();
    initializeChat();
    var chat = new Chat(100, "Test", Timestamp.from(Instant.now()), 1);
    assertFalse(dbManager.chatExists(chat.id()));
  }

  @Test
  void testPromptExists(){
    initializeLLM();
    initializeChat();
    initializePrompt();
    var prompt = prompts.getLast();
    assertTrue(dbManager.promptExists(prompt.id()));
  }

  @Test
  void testPromptNotExists(){
    initializeLLM();
    initializeChat();
    initializePrompt();
    var prompt = new Prompt(100, "Test", AuthorType.USER, 1, false);
    assertFalse(dbManager.promptExists(prompt.id()));
  }

  @Test
  void testGetChatById() {
    initializeLLM();
    initializeChat();
    var chat = chats.getLast();
    var chatById = dbManager.getChatById(chat.id());
    assertEquals(chat, chatById);
  }

  @Test
  void testUpdateChat() {
    initializeLLM();
    initializeChat();
    var chat = chats.getLast();
    var newChat = new Chat(chat.id(), "Test", Timestamp.valueOf("2024-12-24 12:32:59"), 1);
    dbManager.updateChat(newChat);
    var updatedChat = dbManager.getChatById(chat.id());
    assertEquals(newChat, updatedChat);
    assertNotEquals(chat, updatedChat);
  }

  @Test
  void testDeleteChatById() {
    initializeLLM();
    initializeChat();
    var chat = chats.getLast();
    dbManager.deleteChatById(chat.id());
    var chatsDb = dbManager.listChats();
    assertEquals(2, chatsDb.size());
    assertNotEquals(chat, chatsDb.getLast());
  }

  @Test
  void testListLLMs() {
    initializeLLM();
    var llms = dbManager.listLLMs();
    assertEquals(2, llms.size());
  }

  @Test
  void testGetFirstLLM() {
    initializeLLM();
    var llm = llms.getFirst();
    var firstLLM = dbManager.getFirstLLM();
    assertEquals(llm, firstLLM);
  }

  @Test
  void testGetLLMByIdExists() {
    initializeLLM();
    var llm = llms.getFirst();
    var llmById = dbManager.getLLMById(llm.id());
    assertEquals(llm, llmById);
  }

  @Test
  void testGetLLMByIdNotExists() {
    initializeLLM();
    assertThrows(NotFoundException.class, () -> dbManager.getLLMById(100));
  }

  @Test
  void testGetFirstPromptByChatId(){
    initializeLLM();
    initializeChat();
    initializePrompt();
    var chatId = 1;
    var prompt = dbManager.getFirstPromptByChatId(chatId);
    assertEquals(prompts.getFirst(), prompt);
    assertNotEquals(prompts.getLast(), prompt);
  }
}
