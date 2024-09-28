package fr.esiee.app.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import com.wordnik.swagger.models.auth.In;
import fr.esiee.app.db.entities.Chat;
import fr.esiee.app.db.entities.Prompt;
import io.helidon.common.Weight;
import io.helidon.common.context.Contexts;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbExecute;
import io.helidon.dbclient.DbTransaction;
import io.helidon.http.NotFoundException;
import io.helidon.http.Status;
import io.helidon.webserver.http.*;

import java.io.*;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.*;

/**
 * A service that uses {@link DbClient} to manage Prompt, LLM, and Chat tables.
 */
public class DbService {

  private static final Logger LOGGER = System.getLogger(DbService.class.getName());
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final DbClient dbClient;

  public DbService() {
    Config config = Config.global().get("db");
    this.dbClient = Contexts.globalContext()
            .get(DbClient.class)
            .orElseGet(() -> DbClient.create(config));
    try {
      if (!isAppInitialized()) {
        dbInit();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void initLLMs(DbExecute exec) {
    try {
      JsonNode llms = OBJECT_MAPPER.readTree(DbService.class.getResourceAsStream("/llms.json"));
      for (JsonNode llm : llms) {
        exec.namedInsert("insert-llm",
                llm.get("id").asInt(),
                llm.get("name").asText(),
                llm.get("model").asText());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private static void initChats(DbExecute exec) {
    try {
      JsonNode chats = OBJECT_MAPPER.readTree(DbService.class.getResourceAsStream("/chats.json"));
      for (JsonNode chat : chats) {
        exec.namedInsert("insert-chat",
                chat.get("id").asInt(),
                chat.get("title").asText(),
                chat.get("last_activity").asText(),
                chat.get("llm_id").asInt());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void initPrompts(DbExecute exec) {
    try {
      JsonNode prompts = OBJECT_MAPPER.readTree(DbService.class.getResourceAsStream("/prompts.json"));
      for (JsonNode prompt : prompts) {
        exec.namedInsert("insert-prompt",
                prompt.get("id").asInt(),
                prompt.get("message").asText(),
                prompt.get("author_type").asText(),
                prompt.get("llm_response").asText(),
                prompt.get("chat_id").asInt(),
                prompt.get("llm_id").asInt());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isAppInitialized() throws IOException {
    var properties = new Properties();
    var path = Paths.get("src/main/resources/app.properties");
    if (!Files.exists(path)) {
      initializeAppProps(properties, path, StandardOpenOption.CREATE);
      return false;
    }
    try (var input = Files.newInputStream(path)) {
      properties.load(input);
    }
    if (!"true".equals(properties.getProperty("initialized").trim())) {
      initializeAppProps(properties, path, StandardOpenOption.TRUNCATE_EXISTING);
      return false;
    }
    return true;
  }


  private void initializeAppProps(Properties props, Path path, StandardOpenOption option) throws IOException {
    props.setProperty("initialized", "true");
    try (var output = Files.newOutputStream(path, option)) {
      props.store(output, null);
    }
  }

  private void dbInit() {
    initSchema();
    initData();
  }

  private void initSchema() {
    DbExecute exec = dbClient.execute();
    try {
      exec.namedDml("create-llm");
      exec.namedDml("create-chat");
      exec.namedDml("create-prompt");
    } catch (Exception ex1) {
      LOGGER.log(Level.WARNING, "Could not create tables", ex1);
      try {
        deleteData();
      } catch (Exception ex2) {
        LOGGER.log(Level.WARNING, "Could not delete tables", ex2);
      }
    }
  }

  private void initData() {
    DbTransaction tx = dbClient.transaction();
    try {
      initLLMs(tx);
      initChats(tx);
      initPrompts(tx);
      tx.commit();
    } catch (Throwable t) {
      tx.rollback();
      throw t;
    }
  }

  private void deleteData() {
    DbTransaction tx = dbClient.transaction();
    try {
      tx.namedDelete("delete-all-prompts");
      tx.namedDelete("delete-all-llms");
      tx.namedDelete("delete-all-chats");
      tx.commit();
    } catch (Throwable t) {
      tx.rollback();
      throw t;
    }
  }

  public List<Chat> listChats() {
    return dbClient.execute()
            .namedQuery("select-all-chats")
            .map(e -> e.as(Chat.class)).toList();
  }

  public List<Prompt> listPrompts() {
    return dbClient.execute()
            .namedQuery("select-all-prompts")
            .map(e -> e.as(Prompt.class))
            .toList();
  }

  public Prompt getPromptById(int promptId) {
    return dbClient.execute()
            .createNamedGet("select-prompt-by-id")
            .addParam("id", promptId)
            .execute()
            .orElseThrow(() -> new NotFoundException("Prompt " + promptId + " not found"))
            .as(Prompt.class);
  }

  public long insertPrompt(Prompt prompt) {
    if (prompt.id() < 0 || prompt.llmId() < 0 || prompt.chatId() < 0) {
      throw new IllegalArgumentException("id, llmId, or chatId is negative");
    }
    if (promptExists(prompt.id())) {
      throw new IllegalArgumentException("Prompt " + prompt.id() + " already exists");
    }

    var updatedRows = dbClient.execute().createNamedInsert("insert-prompt")
            .addParam(prompt.message())
            .addParam(prompt.authorType().name())
            .addParam(prompt.llmResponse())
            .addParam(prompt.chatId())
            .addParam(prompt.llmId())
            .execute();

    if (updatedRows <= 0) {
      throw new BadRequestException("Failed to insert prompt");
    }
    updateChatLastActivity(prompt.chatId());
    return updatedRows;
  }

  private void updateChatLastActivity(int chatId) {
    dbClient.execute().createNamedUpdate("update-chat-last-activity")
            .addParam("id", chatId)
            .addParam("lastActivity", Timestamp.from(Instant.now()))
            .execute();
  }

  public long updatePrompt(Prompt prompt) {
    if (prompt.id() < 0 || prompt.llmId() < 0 || prompt.chatId() < 0) {
      throw new IllegalArgumentException("id, llmId, or chatId is negative");
    }
    if (!promptExists(prompt.id())) {
      throw new NotFoundException("Prompt " + prompt.id() + " not found");
    }
    return dbClient.execute().createNamedUpdate("update-prompt-by-id").namedParam(prompt).execute();
  }

  public long deletePromptById(int promptId) {
    var count = dbClient.execute().createNamedDelete("delete-prompt-by-id")
            .addParam("id", promptId)
            .execute();
    if (count == 0) {
      throw new NotFoundException("Prompt " + promptId + " not found");
    }
    return count;
  }

  public List<Prompt> getPromptsByChatId(int promptId) {
    return dbClient.execute()
            .createNamedQuery("select-prompts-by-chat-id")
            .addParam("chatId", promptId)
            .execute()
            .map(e -> e.as(Prompt.class))
            .toList();
  }

  public long insertChat(Chat chat) {
    if (chat.id() < 0 || chat.llmId() < 0) {
      throw new IllegalArgumentException("id or llmId is negative");
    }
    if (chatExists(chat.id())) {
      throw new IllegalArgumentException("Chat " + chat.id() + " already exists");
    }
    return dbClient.execute()
            .createNamedInsert("insert-chat")
            .addParam(chat.title())
            .addParam(chat.lastActivity())
            .addParam(chat.llmId()).execute();
  }

  public boolean chatExists(int chatId) {
    return dbClient.execute()
            .createNamedGet("select-chat-by-id")
            .addParam("id", chatId)
            .execute()
            .isPresent();
  }

  public boolean promptExists(int promptId) {
    return dbClient.execute()
            .createNamedGet("select-prompt-by-id")
            .addParam("id", promptId)
            .execute()
            .isPresent();
  }

  public Chat getChatById(int chatId) {
    return dbClient.execute()
            .createNamedGet("select-chat-by-id")
            .addParam("id", chatId)
            .execute()
            .orElseThrow(() -> new NotFoundException("Chat " + chatId + " not found"))
            .as(Chat.class);
  }

  public long updateChat(Chat chat) {
    if (chat.id() < 0 || chat.llmId() < 0) {
      throw new IllegalArgumentException("id or llmId is negative");
    }
    if (!chatExists(chat.id())) {
      throw new NotFoundException("Chat " + chat.id() + " not found");
    }
    return dbClient.execute().createNamedUpdate("update-chat-by-id")
            .namedParam(chat).execute();
  }


  public long deleteChatById(int chatId) {
    var count = dbClient.execute().createNamedDelete("delete-chat-by-id")
            .addParam("id", chatId)
            .execute();
    if (count == 0) {
      throw new NotFoundException("Chat " + chatId + " not found");
    }
    return count;
  }
}
