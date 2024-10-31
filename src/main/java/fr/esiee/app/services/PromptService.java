package fr.esiee.app.services;

import fr.esiee.app.db.DbManager;
import fr.esiee.app.db.entities.AuthorType;
import fr.esiee.app.db.entities.Prompt;
import fr.esiee.app.exception.RestApiException;
import fr.esiee.app.utils.ErrorUtils;
import io.helidon.common.context.Contexts;
import io.helidon.http.Status;
import io.helidon.http.sse.SseEvent;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.sse.SseSink;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.*;
import java.util.NoSuchElementException;


@Tag(name = "Prompts", description = "endpoints to manipulate prompts")
@Path("/api/prompt/")
public class PromptService implements HttpService {

  private final DbManager dbClient;

  public PromptService() {
    this.dbClient = Contexts.globalContext().get(DbManager.class).orElseThrow(() -> new NoSuchElementException("DbManager not found."));
  }

  @Override
  public void routing(HttpRules httpRules) {
    httpRules.get("/", this::listPrompts)
            .get("/byid/{id}", this::getPromptById)
            .get("/bychat/{id}", this::getPromptsByChatId)
            .get("/bychat/{id}/first", this::getFirstPromptByChatId)
            .post("/", Handler.create(Prompt.class, this::insertPrompt))
            .put("/", Handler.create(Prompt.class, this::updatePrompt))
            .delete("/{id}", this::deletePromptById)
            .get("/test/test_progressive", this::testProgressive);
  }

  @GET
  @javax.ws.rs.Path("/")
  @Operation(summary = "List all prompts", description = "Retrieves a list of all prompts")
  @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = Prompt.class)), mediaType = "application/json"), responseCode = "200", description = "Successful operation")
  public void listPrompts(@Parameter(hidden = true) ServerRequest request, @Parameter(hidden = true) ServerResponse response) {
    response.status(Status.OK_200).send(dbClient.listPrompts());
  }

  @POST
  @javax.ws.rs.Path("/")
  @Operation(summary = "Insert a prompt", description = "Inserts a prompt into the database")
  @Consumes("application/json")
  @Schema(name = "AuthorType", implementation = AuthorType.class)
  @ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Prompt.class)), responseCode = "201", description = "Prompt inserted successfully")
  public void insertPrompt(Prompt prompt, @Parameter(hidden = true) ServerResponse response) {
    long insertedRows = dbClient.insertPrompt(prompt);
    if (insertedRows <= 0) {
      ErrorUtils.send(response, Status.BAD_REQUEST_400, "Failed to insert prompt");
      return;
    }
    response.status(Status.CREATED_201).send("Prompt inserted successfully");
  }

  @PUT
  @javax.ws.rs.Path("/")
  @Operation(summary = "Update a prompt", description = "Updates a prompt in the database")
  @Consumes("application/json")
  @ApiResponse(content = @Content(mediaType = "text/plain"), responseCode = "200", description = "Successful operation")
  public void updatePrompt(Prompt prompt, @Parameter(hidden = true) ServerResponse response) {
    long updatedRows = dbClient.updatePrompt(prompt);
    if (updatedRows <= 0) {
      ErrorUtils.send(response, Status.BAD_REQUEST_400, "Failed to update prompt");
      return;
    }
    response.status(Status.OK_200).send("Prompt updated successfully");
  }

  @DELETE
  @javax.ws.rs.Path("/{id}")
  @Operation(summary = "Delete a prompt by ID", description = "Deletes a prompt by its ID")
  @ApiResponse(content = @Content(mediaType = "text/plain"), responseCode = "200", description = "Successful operation")
  public void deletePromptById(
          @Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the prompt to delete", required = true, schema = @Schema(type = "integer", description = "Chat ID", example = "1")) ServerRequest request,
          @Parameter(hidden = true) ServerResponse response
  ) {
    int promptId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new RestApiException("Prompt ID is required"));
    long deletedRows = dbClient.deletePromptById(promptId);
    response.status(Status.OK_200).send("Deleted " + deletedRows + " rows");
  }

  @GET
  @javax.ws.rs.Path("/{id}")
  @Operation(summary = "Get prompt by ID", description = "Retrieves a prompt by its ID")
  @ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Prompt.class)), responseCode = "200", description = "Successful operation")
  public void getPromptById(
          @Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the prompt to retrieve", required = true, schema = @Schema(type = "integer", description = "Chat ID", example = "1")) ServerRequest request,
          @Parameter(hidden = true) ServerResponse response
  ) {
    int promptId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new RestApiException("Prompt ID is required"));
    var prompt = dbClient.getPromptById(promptId);
    response.status(Status.OK_200).send(prompt);
  }

  @GET
  @javax.ws.rs.Path("/bychat/{id}")
  @Operation(summary = "List prompts by chat ID", description = "Retrieves a list of prompts by chat ID")
  @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = Prompt.class)), mediaType = "application/json"), responseCode = "200", description = "Successful operation")
  public void getPromptsByChatId(
          @Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the chat used to retrieve prompts", required = true, schema = @Schema(type = "integer", description = "Chat ID", example = "1")) ServerRequest request,
          @Parameter(hidden = true) ServerResponse response
  ) {
    int chatId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new RestApiException("Chat ID is required"));
    var prompts = dbClient.getPromptsByChatId(chatId);
    response.status(Status.OK_200).send(prompts);
  }

  @GET
  @javax.ws.rs.Path("/bychat/{id}/first")
  @Operation(summary = "Get first prompt by chat ID", description = "Retrieves the first prompt by chat ID")
  @ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Prompt.class)), responseCode = "200", description = "Successful operation")
  public void getFirstPromptByChatId(
          @Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the chat used to retrieve first prompt", required = true, schema = @Schema(type = "integer", description = "Chat ID", example = "1")) ServerRequest request,
          @Parameter(hidden = true) ServerResponse response
  ) {
    int chatId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new RestApiException("Chat ID is required"));
    var prompt = dbClient.getFirstPromptByChatId(chatId);
    response.status(Status.OK_200).send(prompt);
  }

  public void testProgressive(ServerRequest request, ServerResponse response) {
    try (SseSink sseSink = response.sink(SseSink.TYPE)) {
      for (var i = 0; i < 10; i++) {
        sseSink.emit(SseEvent.create("word " + i));
        Thread.sleep(1000);
      }
      // Send a final event to indicate the end of the sequence
      sseSink.emit(SseEvent.create("done"));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
