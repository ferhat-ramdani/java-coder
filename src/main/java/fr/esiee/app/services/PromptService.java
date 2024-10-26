package fr.esiee.app.services;

import fr.esiee.app.db.DbManager;
import fr.esiee.app.db.entities.Prompt;
import io.helidon.common.context.Contexts;
import io.helidon.cors.CrossOriginConfig;
import io.helidon.http.NotFoundException;
import io.helidon.http.Status;
import io.helidon.webserver.cors.CorsSupport;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.*;
import java.util.NoSuchElementException;

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
@Tag(name = "Prompts", description = "endpoints to manipulate prompts")
@Path("/api/prompt/")
public class PromptService implements HttpService {

  private final DbManager dbClient;

  public PromptService() {
    this.dbClient = Contexts.globalContext().get(DbManager.class).orElseThrow(() -> new NoSuchElementException("DbManager not found."));
  }

  @Override
  public void routing(HttpRules httpRules) {
    CorsSupport corsSupport = CorsSupport.builder()
            .addCrossOrigin(CrossOriginConfig.builder()
                    .allowOrigins("*")
                    .allowMethods("*")
                    .build())
            .addCrossOrigin(CrossOriginConfig.create())
            .build();
    httpRules.get("/", this::listPrompts)
            .get("/{id}", this::getPromptById)
            .get("/bychat/{id}", this::getPromptsByChatId)
            .get("/bychat/{id}/first", corsSupport, this::getFirstPromptByChatId)
            .post("/", Handler.create(Prompt.class, this::insertPrompt))
            .put("/", Handler.create(Prompt.class, this::updatePrompt))
            .delete("/{id}", this::deletePromptById);
  }

  @GET
  @javax.ws.rs.Path("/")
  @Operation(summary = "List all prompts", description = "Retrieves a list of all prompts")
  public void listPrompts(@Parameter(hidden = true) ServerRequest request, @Parameter(hidden = true) ServerResponse response) {
    response.send(dbClient.listPrompts());
  }

  @POST
  @javax.ws.rs.Path("/")
  @Operation(summary = "Insert a prompt", description = "Inserts a prompt into the database")
  public void insertPrompt(Prompt prompt, @Parameter(hidden = true) ServerResponse response) {
    long insertedRows = dbClient.insertPrompt(prompt);
    if (insertedRows <= 0) {
      response.status(Status.BAD_REQUEST_400).send("Failed to insert prompt");
      return;
    }
    response.status(Status.CREATED_201).send("Prompt inserted successfully");
  }

  @PUT
  @javax.ws.rs.Path("/")
  @Operation(summary = "Update a prompt", description = "Updates a prompt in the database")
  public void updatePrompt(Prompt prompt, @Parameter(hidden = true) ServerResponse response) {
    long updatedRows = dbClient.updatePrompt(prompt);
    if (updatedRows <= 0) {
      response.status(Status.BAD_REQUEST_400).send("Failed to update prompt");
      return;
    }
    response.status(Status.OK_200).send("Prompt updated successfully");
  }

  @DELETE
  @javax.ws.rs.Path("/{id}")
  @Operation(summary = "Delete a prompt by ID", description = "Deletes a prompt by its ID")
  public void deletePromptById(
          @Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the prompt to delete", required = true, schema = @Schema(type = "integer", description = "Chat ID", example = "1")) ServerRequest request,
          @Parameter(hidden = true) ServerResponse response
  ) {
    int promptId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new NotFoundException("Prompt ID is required"));
    long deletedRows = dbClient.deletePromptById(promptId);
    response.status(Status.OK_200).send("Deleted " + deletedRows + " rows");
  }

  @GET
  @javax.ws.rs.Path("/{id}")
  @Operation(summary = "Get prompt by ID", description = "Retrieves a prompt by its ID")
  public void getPromptById(
          @Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the prompt to retrieve", required = true, schema = @Schema(type = "integer", description = "Chat ID", example = "1")) ServerRequest request,
          @Parameter(hidden = true) ServerResponse response
  ) {
    int promptId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new NotFoundException("Prompt ID is required"));
    var prompt = dbClient.getPromptById(promptId);
    response.send(prompt);
  }

  @GET
  @javax.ws.rs.Path("/bychat/{id}")
  @Operation(summary = "List prompts by chat ID", description = "Retrieves a list of prompts by chat ID")
  public void getPromptsByChatId(
          @Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the chat used to retrieve prompts", required = true, schema = @Schema(type = "integer", description = "Chat ID", example = "1")) ServerRequest request,
          @Parameter(hidden = true) ServerResponse response
  ) {
    int chatId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new NotFoundException("Chat ID is required"));
    var prompts = dbClient.getPromptsByChatId(chatId);
    response.send(prompts);
  }

  @GET
  @javax.ws.rs.Path("/bychat/{id}/first")
  @Operation(summary = "Get first prompt by chat ID", description = "Retrieves the first prompt by chat ID")
  public void getFirstPromptByChatId(
          @Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the chat used to retrieve first prompt", required = true, schema = @Schema(type = "integer", description = "Chat ID", example = "1")) ServerRequest request,
          @Parameter(hidden = true) ServerResponse response
  ) {
    int chatId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new NotFoundException("Chat ID is required"));
    var prompt = dbClient.getFirstPromptByChatId(chatId);
    response.send(prompt);
  }
}
