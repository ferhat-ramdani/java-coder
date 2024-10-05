package fr.esiee.app.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.esiee.app.db.entities.Chat;
import fr.esiee.app.db.entities.LLM;
import fr.esiee.app.db.entities.Prompt;
import io.helidon.common.context.Contexts;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbExecute;
import io.helidon.dbclient.DbTransaction;
import io.helidon.http.NotFoundException;

import javax.ws.rs.BadRequestException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * A service that uses {@link DbClient} to manage Prompt, LLM, and Chat.ts tables.
 */
public class DbService {

  private static final Logger LOGGER = System.getLogger(DbService.class.getName());
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static DbService instance;
  private final DbClient dbClient;

  public DbService() {
    Config config = Config.global().get("db");
    this.dbClient = Contexts.globalContext()
            .get(DbClient.class)
            .orElseGet(() -> DbClient.create(config));

    initSchema();

    if (getLLMCount() <= 0) {
      initData();
    }
  }

  private static void initLLMs(DbExecute exec) {
    try {
      JsonNode llms = OBJECT_MAPPER.readTree(DbService.class.getResourceAsStream("/llms.json"));
      for (JsonNode llm : llms) {
        exec.namedInsert("insert-llm",
                llm.get("name").asText(),
                llm.get("model").asText(),
                llm.get("system_prompt").asText(""),
                llm.get("caracteristics").asText(""));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public static synchronized DbService getInstance() {
    if (instance == null) {
      instance = new DbService();
    }
    return instance;
  }

  private int getLLMCount() {
    return dbClient.execute()
            .namedQuery("select-all-llms")
            .map(e -> e.as(LLM.class))
            .toList().size();
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
    if (prompt.id() < 0 || prompt.chatId() < 0) {
      throw new IllegalArgumentException("id, llmId, or chatId is negative");
    }
    if (promptExists(prompt.id())) {
      throw new IllegalArgumentException("Prompt " + prompt.id() + " already exists");
    }

    var updatedRows = dbClient.execute().createNamedInsert("insert-prompt")
            .addParam(prompt.message())
            .addParam(prompt.authorType().name())
            .addParam(prompt.chatId())
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
    if (prompt.id() < 0 || prompt.chatId() < 0) {
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
    return dbClient.execute()
            .createNamedInsert("insert-chat")
            .addParam(chat.title())
            .addParam(chat.lastActivity())
            .addParam(chat.llmId()).execute();
  }

  public Chat getLatestChat(Chat chat) {
    return dbClient.execute()
            .createNamedGet("select-chat-by-params")
            .addParam("title", chat.title())
            .addParam("last_activity", chat.lastActivity())
            .addParam("llm_id", chat.llmId())
            .execute()
            .orElseThrow(() -> new NotFoundException("Chat " + chat + " not found"))
            .as(Chat.class);
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
      throw new NotFoundException("Chat.ts " + chatId + " not found");
    }
    return count;
  }

  public List<LLM> listLLMs() {
    return dbClient.execute()
            .namedQuery("select-all-llms")
            .map(e -> e.as(LLM.class))
            .toList();
  }

  public LLM getLLMById(int llmId) {
    return dbClient.execute()
            .createNamedGet("select-llm-by-id")
            .addParam("id", llmId)
            .execute()
            .orElseThrow(() -> new NotFoundException("LLM " + llmId + " not found"))
            .as(LLM.class);
  }

  public Prompt getFirstPromptByChatId(int chatId) {
    return dbClient.execute()
            .createNamedQuery("select-first-prompt-by-chat-id")
            .addParam("chatId", chatId)
            .execute()
            .map(e -> e.as(Prompt.class))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Chat " + chatId + " not found"));
  }
}
