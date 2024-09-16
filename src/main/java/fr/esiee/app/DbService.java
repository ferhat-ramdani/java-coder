package fr.esiee.app;

import fr.esiee.app.db.Chat;
import fr.esiee.app.db.Prompt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.Properties;

/**
 * A service that uses {@link DbClient} to manage Prompt, LLM, and Chat tables.
 */
@Weight(100)
public class DbService implements HttpService {

  private static final Logger LOGGER = System.getLogger(DbService.class.getName());
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final DbClient dbClient;

  /**
   * Create a new service with a DB client.
   */
  DbService() {
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
                llm.get("name").asText());
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
                chat.get("title").asText());
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
                prompt.get("user_message").asText(),
                prompt.get("llm_response").asText(),
                prompt.get("id_llm").asInt(),
                prompt.get("id_chat").asInt());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isAppInitialized() throws IOException {
    var isInit = true;
    var properties = new Properties();
    var path = Paths.get("app.properties");
    if (!Files.exists(path)) {
      isInit = false;
      initializeAppProps(properties, path, StandardOpenOption.CREATE);
    } else {
      try (var input = new ByteArrayInputStream(Files.readAllBytes(path))) {
        properties.load(input);
      }
      if (!"true".equals(properties.getProperty("initialized"))) {
        isInit = false;
        initializeAppProps(properties, path, StandardOpenOption.TRUNCATE_EXISTING);
      }
    }
    return isInit;
  }

  private void initializeAppProps(Properties props, Path path, StandardOpenOption option) throws IOException {
    props.setProperty("initialized", "true");
    try (var output = new ByteArrayOutputStream()) {
      props.store(output, null);
      Files.write(path, output.toByteArray(), option);
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

  @Override
  public void routing(HttpRules rules) {
    rules.get("/", this::index)
            // List all LLMs
            .get("/llm", this::listLLMs)
            // List all Chats
            .get("/chat", this::listChats)
            // List all Prompts
            .get("/prompt", this::listPrompts)
            // Get Prompt by ID
            .get("/prompt/{id}", this::getPromptById)
            // Create new Prompt
            .post("/prompt", Handler.create(Prompt.class, this::insertPrompt))
            // Create new chat
            .post("/chat", Handler.create(Chat.class, this::insertChat))
            // Update existing Prompt
            .put("/prompt", Handler.create(Prompt.class, this::updatePrompt))
            // Update existing Chat
            .put("/chat", Handler.create(Chat.class, this::updateChat))
            // Delete Prompt by ID
            .delete("/prompt/{id}", this::deletePromptById)
            // Delete Chat by ID
            .delete("/chat/{id}", this::deleteChatById)
            // Get Prompts by Chat ID
            .get("/chat/{id}/prompts", this::getPromptsByChatId);
  }

  private void index(ServerRequest request, ServerResponse response) {
    response.headers().contentType(MediaTypes.TEXT_PLAIN);
    response.send("""
             DB Service Example:
                  GET /llm                  - List all LLMs
                  GET /chat                 - List all Chats
                  GET /prompt               - List all Prompts
                  GET /prompt/{id}          - Get Prompt by ID
                  POST /prompt              - Insert new Prompt
                  PUT /prompt               - Update Prompt
                  DELETE /prompt/{id}       - Delete Prompt by ID
            
                  GET /chat/{id}/prompts    - List all Prompts for a Chat
                  POST /chat                - Insert new Chat
                  PUT /chat                 - Update Chat
                  DELETE /chat/{id}         - Delete Chat by ID
            """);
  }

  private void listLLMs(ServerRequest request, ServerResponse response) {
    ArrayNode llmsArray = OBJECT_MAPPER.createArrayNode();
    dbClient.execute()
            .namedQuery("select-all-llms")
            .forEach(row -> {
              ObjectNode llmJson = OBJECT_MAPPER.createObjectNode()
                      .put("id", row.column("id").getInt())
                      .put("name", row.column("name").getString());
              llmsArray.add(llmJson);
            });
    response.send(llmsArray);
  }

  private void listChats(ServerRequest request, ServerResponse response) {
    ArrayNode chatsArray = OBJECT_MAPPER.createArrayNode();
    dbClient.execute()
            .namedQuery("select-all-chats")
            .forEach(row -> {
              ObjectNode chatJson = OBJECT_MAPPER.createObjectNode()
                      .put("id", row.column("id").getInt())
                      .put("title", row.column("title").getString());
              chatsArray.add(chatJson);
            });
    response.send(chatsArray);
  }

  private void listPrompts(ServerRequest request, ServerResponse response) {
    ArrayNode promptsArray = OBJECT_MAPPER.createArrayNode();
    dbClient.execute()
            .namedQuery("select-all-prompts")
            .forEach(row -> {
              ObjectNode promptJson = OBJECT_MAPPER.createObjectNode()
                      .put("id", row.column("id").getInt())
                      .put("user_message", row.column("user_message").getString())
                      .put("llm_response", row.column("llm_response").getString())
                      .put("id_chat", row.column("id_chat").getInt())
                      .put("id_llm", row.column("id_llm").getInt());
              promptsArray.add(promptJson);
            });
    response.send(promptsArray);
  }

  private void getPromptById(ServerRequest request, ServerResponse response) {
    int promptId = Integer.parseInt(request.path().pathParameters().get("id"));
    ObjectNode promptJson = dbClient.execute()
            .createNamedGet("select-prompt-by-id")
            .addParam("id", promptId)
            .execute()
            .map(row -> OBJECT_MAPPER.createObjectNode()
                    .put("id", row.column("id").getInt())
                    .put("user_message", row.column("user_message").getString())
                    .put("llm_response", row.column("llm_response").getString())
                    .put("id_chat", row.column("id_chat").getInt())
                    .put("id_llm", row.column("id_llm").getInt()))
            .orElseThrow(() -> new NotFoundException("Prompt " + promptId + " not found"));

    response.send(promptJson);
  }

  private void insertPrompt(Prompt prompt, ServerResponse response) {
    dbClient.execute()
            .namedInsert("insert-prompt", prompt.id(), prompt.userMessage(), prompt.llmResponse(),
                    prompt.llmId(), prompt.chatId());

    response.status(Status.CREATED_201).send();
  }

  private void updatePrompt(Prompt prompt, ServerResponse response) {
    dbClient.execute()
            .namedUpdate("update-prompt", prompt.userMessage(), prompt.llmResponse(),
                    prompt.llmId(), prompt.chatId(), prompt.id());

    response.status(Status.NO_CONTENT_204).send();
  }

  private void deletePromptById(ServerRequest request, ServerResponse response) {
    int promptId = Integer.parseInt(request.path().pathParameters().get("id"));
    long count = dbClient.execute()
            .namedDelete("delete-prompt-by-id", promptId);

    if (count == 0) {
      throw new NotFoundException("Prompt " + promptId + " not found");
    }
    response.status(Status.NO_CONTENT_204).send();
  }

  private void getPromptsByChatId(ServerRequest request, ServerResponse response) {
    int chatId = Integer.parseInt(request.path().pathParameters().get("id"));
    ArrayNode promptsArray = OBJECT_MAPPER.createArrayNode();

    dbClient.execute().createNamedQuery("select-prompts-by-chat-id")
            .addParam("id_chat", chatId)
            .execute()
            .forEach(row -> {
              ObjectNode promptJson = OBJECT_MAPPER.createObjectNode()
                      .put("id", row.column("id").getInt())
                      .put("user_message", row.column("user_message").getString())
                      .put("llm_response", row.column("llm_response").getString())
                      .put("id_chat", row.column("id_chat").getInt())
                      .put("id_llm", row.column("id_llm").getInt());
              promptsArray.add(promptJson);
            });

    response.send(promptsArray);
  }

  private void insertChat(Chat chat, ServerResponse response) {
    dbClient.execute()
            .namedInsert("insert-chat", chat.id(), chat.title());

    response.status(Status.CREATED_201).send();
  }

  private void updateChat(Chat chat, ServerResponse response) {
    dbClient.execute()
            .namedUpdate("update-chat", chat.title(), chat.id());

    response.status(Status.NO_CONTENT_204).send();
  }

  private void deleteChatById(ServerRequest request, ServerResponse response) {
    int chatId = Integer.parseInt(request.path().pathParameters().get("id"));
    long count = dbClient.execute()
            .namedDelete("delete-chat-by-id", chatId);

    if (count == 0) {
      throw new NotFoundException("Chat " + chatId + " not found");
    }
    response.status(Status.NO_CONTENT_204).send();
  }
}
