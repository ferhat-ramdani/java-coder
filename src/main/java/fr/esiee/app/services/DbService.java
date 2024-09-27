package fr.esiee.app.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import fr.esiee.app.db.entities.Chat;
import fr.esiee.app.db.entities.LLM;
import fr.esiee.app.db.entities.Prompt;
import fr.esiee.app.db.mapper.PromptMapper;
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

import javax.ws.rs.*;

/**
 * A service that uses {@link DbClient} to manage Prompt, LLM, and Chat tables.
 */
@Weight(100)
@javax.ws.rs.Path("/db")
@Api(value="/db", description = "Operations about database")
@Produces({"application/json"})
public class DbService implements HttpService {

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
                chat.get("last_activity_timestamp").asText(),
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
                  PUT /prompt               - Update existing Prompt
                  DELETE /prompt/{id}       - Delete Prompt by ID
            
                  GET /chat/{id}/prompts    - List all Prompts for a Chat
                  POST /chat                - Insert new Chat
                  PUT /chat                 - Update existing Chat (requires id, title, last_activity_timestamp, llm_id)
                  DELETE /chat/{id}         - Delete Chat by ID
            """);
  }


  @GET
  @javax.ws.rs.Path("/llms")
  @ApiOperation(value = "List all LLMs", response = LLM.class, responseContainer = "List")
  public void listLLMs(ServerRequest request, ServerResponse response) {
    var llmsList = dbClient.execute()
            .namedQuery("select-all-llms")
            .map(e -> e.as(LLM.class))
            .toList();
    response.send(llmsList);
  }

  @GET
  @javax.ws.rs.Path("/chat")
  @ApiOperation(value = "List all Chats", response = Chat.class, responseContainer = "List")
  public void listChats(ServerRequest request, ServerResponse response) {
    var chatsArray = dbClient.execute()
            .namedQuery("select-all-chats")
            .map(e -> e.as(Chat.class)).toList();
    response.send(chatsArray);
  }

  @GET
  @javax.ws.rs.Path("/prompts")
  @ApiOperation(value = "List all Prompts", response = Prompt.class, responseContainer = "List")
  public void listPrompts(ServerRequest request, ServerResponse response) {
    var promptsArray = dbClient.execute()
            .namedQuery("select-all-prompts")
            .map(e -> e.as(Prompt.class))
            .toList();
    response.send(promptsArray);
  }

  @GET
  @javax.ws.rs.Path("/prompts/{id}")
  @ApiOperation(value = "Get a Prompt by ID", response = Prompt.class)
  @ApiResponses(value = {
          @ApiResponse(code = 404, message = "Prompt not found")
  })
  public void getPromptById(ServerRequest request, ServerResponse response) {
    int promptId = Integer.parseInt(request.path().pathParameters().get("id"));
    Prompt prompt = dbClient.execute()
            .createNamedGet("select-prompt-by-id")
            .addParam("id", promptId)
            .execute()
            .orElseThrow(() -> new NotFoundException("Prompt " + promptId + " not found"))
            .as(Prompt.class);
    response.send(prompt);
  }

  @POST
  @javax.ws.rs.Path("/prompts")
  @ApiOperation(value = "Insert a new Prompt")
  public void insertPrompt(Prompt prompt, ServerResponse response) {
    dbClient.execute()
            .namedInsert("insert-prompt",
                    prompt.id(),
                    prompt.message(),
                    prompt.authorType().name(),
                    prompt.llmResponse(),
                    prompt.chatId(),
                    prompt.llmId());
    response.status(Status.CREATED_201).send();
  }

  @PUT
  @javax.ws.rs.Path("/prompts")
  @ApiOperation(value = "Update an existing Prompt")
  public void updatePrompt(Prompt prompt, ServerResponse response) {
    dbClient.execute()
            .namedUpdate("update-prompt",
                    prompt.message(),
                    prompt.authorType().name(),
                    prompt.llmResponse(),
                    prompt.chatId(),
                    prompt.llmId(),
                    prompt.id());
    response.status(Status.NO_CONTENT_204).send();
  }

  @DELETE
  @javax.ws.rs.Path("/prompts/{id}")
  @ApiOperation(value = "Delete a Prompt by ID")
  @ApiResponses(value = {
          @ApiResponse(code = 404, message = "Prompt not found")
  })
  public void deletePromptById(ServerRequest request, ServerResponse response) {
    int promptId = Integer.parseInt(request.path().pathParameters().get("id"));
    long count = dbClient.execute()
            .namedDelete("delete-prompt-by-id", promptId);

    if (count == 0) {
      throw new NotFoundException("Prompt " + promptId + " not found");
    }
    response.status(Status.NO_CONTENT_204).send();
  }

  @GET
  @javax.ws.rs.Path("/chats/{id}/prompts")
  @ApiOperation(value = "Get Prompts by Chat ID", responseContainer = "List")
  public void getPromptsByChatId(ServerRequest request, ServerResponse response) {
    int promptId = Integer.parseInt(request.path().pathParameters().get("id"));
    Prompt prompt = dbClient.execute()
            .createNamedGet("select-prompt-by-id")
            .addParam("id", promptId)
            .execute()
            .orElseThrow(() -> new NotFoundException("Prompt " + promptId + " not found"))
            .as(Prompt.class);
    response.send(prompt);
  }

  @POST
  @javax.ws.rs.Path("/chats")
  @ApiOperation(value = "Insert a new Chat")
  public void insertChat(Chat chat, ServerResponse response) {
    dbClient.execute()
            .namedInsert("insert-chat",
                    chat.id(),
                    chat.title(),
                    chat.lastAcitivityTimestamp(),
                    chat.llmId());
    response.status(Status.CREATED_201).send();
  }

  @PUT
  @javax.ws.rs.Path("/chats")
  @ApiOperation(value = "Update an existing Chat")
  public void updateChat(Chat chat, ServerResponse response) {
    dbClient.execute()
            .namedUpdate("update-chat",
                    chat.title(),
                    chat.lastAcitivityTimestamp(),
                    chat.llmId(),
                    chat.id());
    response.status(Status.NO_CONTENT_204).send();
  }

  @DELETE
  @javax.ws.rs.Path("/chats/{id}")
  @ApiOperation(value = "Delete a Chat by ID")
  @ApiResponses(value = {
          @ApiResponse(code = 404, message = "Chat not found")
  })
  public void deleteChatById(ServerRequest request, ServerResponse response) {
    int chatId = Integer.parseInt(request.path().pathParameters().get("id"));
    long count = dbClient.execute()
            .namedDelete("delete-chat-by-id", chatId);

    if (count == 0) {
      throw new NotFoundException("Chat " + chatId + " not found");
    }
    response.status(Status.NO_CONTENT_204).send();
  }
}
