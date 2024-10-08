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
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.*;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@OpenAPIDefinition(
        info = @Info(
                title = "GPT for dev API services",
                description = "Services for manipulating chats, prompts and llms"
        ),
        servers = {
                @Server(
                        description = "localhost",
                        url = "http://localhost:8080")
        }
)
@Path("/api")
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
    instance = this;
    Contexts.globalContext().register(instance);
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

  public static synchronized DbService getInstance() {
    if (instance == null) {
      instance = new DbService();
      Contexts.globalContext().register(instance);
    }
    return instance;
  }

  private int getLLMCount() {
    return dbClient.execute()
            .namedQuery("select-all-llms")
            .map(e -> e.as(LLM.class))
            .toList().size();
  }

  @GET
  @javax.ws.rs.Path("/chat/")
  @Tag(name = "Chat", description = "endpoints to manipulate chats")
  @Operation(summary = "List all chats", description = "Retrieves a list of all chats")
  public List<Chat> listChats() {
    return dbClient.execute()
            .namedQuery("select-all-chats")
            .map(e -> e.as(Chat.class)).toList();
  }

  @GET
  @javax.ws.rs.Path("/prompt/")
  @Tag(name = "Prompts", description = "endpoints to manipulate prompts")
  @Operation(summary = "List all prompts", description = "Retrieves a list of all prompts")
  public List<Prompt> listPrompts() {
    return dbClient.execute()
            .namedQuery("select-all-prompts")
            .map(e -> e.as(Prompt.class))
            .toList();
  }

  @GET
  @javax.ws.rs.Path("/prompt/{id}")
  @Tag(name = "Prompts", description = "endpoints to manipulate prompts")
  @Operation(summary = "Get prompt by ID", description = "Retrieves a prompt by its ID")
  public Prompt getPromptById(@PathParam("id") int promptId) {
    return dbClient.execute()
            .createNamedGet("select-prompt-by-id")
            .addParam("id", promptId)
            .execute()
            .orElseThrow(() -> new NotFoundException("Prompt " + promptId + " not found"))
            .as(Prompt.class);
  }

  @POST
  @javax.ws.rs.Path("/prompt/")
  @Tag(name = "Prompts", description = "endpoints to manipulate prompts")
  @Operation(summary = "Insert a prompt", description = "Inserts a prompt into the database")
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

  @PUT
  @javax.ws.rs.Path("/prompt/")
  @Tag(name = "Prompts", description = "endpoints to manipulate prompts")
  @Operation(summary = "Update a prompt", description = "Updates a prompt in the database")
  public long updatePrompt(Prompt prompt) {
    if (prompt.id() < 0 || prompt.chatId() < 0) {
      throw new IllegalArgumentException("id, llmId, or chatId is negative");
    }
    if (!promptExists(prompt.id())) {
      throw new NotFoundException("Prompt " + prompt.id() + " not found");
    }
    return dbClient.execute().createNamedUpdate("update-prompt-by-id").namedParam(prompt).execute();
  }

  @DELETE
  @javax.ws.rs.Path("/prompt/{id}")
  @Tag(name = "Prompts", description = "endpoints to manipulate prompts")
  @Operation(summary = "Delete a prompt by ID", description = "Deletes a prompt by its ID")
  public long deletePromptById(@PathParam("id") int promptId) {
    var count = dbClient.execute().createNamedDelete("delete-prompt-by-id")
            .addParam("id", promptId)
            .execute();
    if (count == 0) {
      throw new NotFoundException("Prompt " + promptId + " not found");
    }
    return count;
  }

  @GET
  @javax.ws.rs.Path("/prompt/bychat/{id}")
  @Tag(name = "Prompts", description = "endpoints to manipulate prompts")
  @Operation(summary = "List prompts by chat ID", description = "Retrieves a list of prompts by chat ID")
  public List<Prompt> getPromptsByChatId(@PathParam("id") int promptId) {
    return dbClient.execute()
            .createNamedQuery("select-prompts-by-chat-id")
            .addParam("chatId", promptId)
            .execute()
            .map(e -> e.as(Prompt.class))
            .toList();
  }

  @POST
  @javax.ws.rs.Path("/chat")
  @Tag(name = "Chat", description = "endpoints to manipulate chats")
  @Operation(summary = "Insert a chat", description = "Inserts a chat into the database")
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

  @GET
  @javax.ws.rs.Path("/chat/latest")
  @Operation(summary = "Get latest chat", description = "Retrieves the latest chat with the specified parameters")
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

  @GET
  @javax.ws.rs.Path("/chat/{id}")
  @Operation(summary = "Check if chat exists", description = "Checks if a chat exists by its ID")
  public boolean chatExists(@PathParam("id") int chatId) {
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

  @GET
  @javax.ws.rs.Path("/chat/{id}")
  @Tag(name = "Prompts", description = "endpoints to manipulate prompts")
  @Operation(summary = "Get chat by ID", description = "Retrieves a chat by its ID")
  public Chat getChatById(@PathParam("id") int chatId) {
    return dbClient.execute()
            .createNamedGet("select-chat-by-id")
            .addParam("id", chatId)
            .execute()
            .orElseThrow(() -> new NotFoundException("Chat " + chatId + " not found"))
            .as(Chat.class);
  }

  @PUT
  @javax.ws.rs.Path("/chat/")
  @Tag(name = "Chat", description = "endpoints to manipulate chats")
  @Operation(summary = "Update a chat", description = "Updates a chat in the database")
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

  @DELETE
  @javax.ws.rs.Path("/chat/{id}")
  @Tag(name = "Chat", description = "endpoints to manipulate chats")
  @Operation(summary = "Delete a chat by ID", description = "Deletes a chat by its ID")
  public long deleteChatById(@PathParam("id") int chatId) {
    var count = dbClient.execute().createNamedDelete("delete-chat-by-id")
            .addParam("id", chatId)
            .execute();
    if (count == 0) {
      throw new NotFoundException("Chat.ts " + chatId + " not found");
    }
    return count;
  }

  @GET
  @javax.ws.rs.Path("/llms")
  @Operation(summary = "List all LLMs", description = "Retrieves a list of all LLMs")
  public List<LLM> listLLMs() {
    return dbClient.execute()
            .namedQuery("select-all-llms")
            .map(e -> e.as(LLM.class))
            .toList();
  }

  @GET
  @javax.ws.rs.Path("/llms/{id}")
  @Operation(summary = "Get LLM by ID", description = "Retrieves an LLM by its ID")
  public LLM getLLMById(@PathParam("id") int llmId) {
    return dbClient.execute()
            .createNamedGet("select-llm-by-id")
            .addParam("id", llmId)
            .execute()
            .orElseThrow(() -> new NotFoundException("LLM " + llmId + " not found"))
            .as(LLM.class);
  }

  @GET
  @javax.ws.rs.Path("/prompt/bychat/{id}/first")
  @Tag(name = "Prompts", description = "endpoints to manipulate prompts")
  @Operation(summary = "Get first prompt by chat ID", description = "Retrieves the first prompt by chat ID")
  public Prompt getFirstPromptByChatId(@PathParam("id") int chatId) {
    return dbClient.execute()
            .createNamedQuery("select-first-prompt-by-chat-id")
            .addParam("chatId", chatId)
            .execute()
            .map(e -> e.as(Prompt.class))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Chat " + chatId + " not found"));
  }
}
