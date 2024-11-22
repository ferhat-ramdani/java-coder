package fr.esiee.app.services;

import fr.esiee.app.db.DbManager;
import fr.esiee.app.db.entities.AuthorType;
import fr.esiee.app.db.entities.Prompt;
import fr.esiee.app.exception.RestApiException;
import fr.esiee.app.utils.ErrorUtils;
import io.helidon.common.context.Contexts;
import io.helidon.http.Status;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.util.NoSuchElementException;

/**
 * A service class that provides endpoints to manipulate prompts.
 */
@Tag(name = "Prompts", description = "endpoints to manipulate prompts")
@Path("/api/prompt/")
public class PromptService implements HttpService {

  private final DbManager dbClient;

  /**
   * Constructs a new PromptService instance.
   * Initializes the DbManager from the global context.
   *
   * @throws NoSuchElementException if DbManager is not found in the global context.
   */
  public PromptService() {
    this.dbClient = Contexts.globalContext().get(DbManager.class)
            .orElseThrow(() -> new NoSuchElementException("DbManager not found."));
  }

  /**
   * Configures the routing rules for the HTTP service.
   *
   * @param httpRules the HTTP rules to configure
   */
  @Override
  public void routing(HttpRules httpRules) {
    httpRules.get("/", this::listPrompts)
            .post("/", Handler.create(Prompt.class, this::insertPrompt))
            .put("/", Handler.create(Prompt.class, this::updatePrompt))
            .get("/bychat/{id}/first", this::getFirstPromptByChatId)
            .get("/bychat/{id}", this::getPromptsByChatId)
            .get("/{id}", this::getPromptById)
            .delete("/{id}", this::deletePromptById);
  }

  /**
   * Handles HTTP GET requests to list all prompts.
   *
   * @param request  the server request
   * @param response the server response
   */
  @GET
  @javax.ws.rs.Path("/")
  @Operation(summary = "List all prompts", description = "Retrieves a list of all prompts")
  @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = Prompt.class)), mediaType = "application/json"), responseCode = "200", description = "Successful operation")
  public void listPrompts(@Parameter(hidden = true) ServerRequest request,
                          @Parameter(hidden = true) ServerResponse response) {
    response.status(Status.OK_200).send(dbClient.listPrompts());
  }

  /**
   * Handles HTTP POST requests to insert a new prompt.
   *
   * @param prompt   the prompt to insert
   * @param response the server response
   */
  @POST
  @javax.ws.rs.Path("/")
  @Operation(summary = "Insert a prompt", description = "Inserts a prompt into the database")
  @Consumes("application/json")
  @Schema(name = "AuthorType", implementation = AuthorType.class)
  @ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Prompt.class)), responseCode = "201", description = "Prompt inserted successfully")
  public void insertPrompt(Prompt prompt, @Parameter(hidden = true) ServerResponse response) {
    var insertedRows = dbClient.insertPrompt(prompt);
    dbClient.updateChatLastActivity(prompt.chatId());
    if (insertedRows <= 0) {
      ErrorUtils.send(response, Status.BAD_REQUEST_400, "Failed to insert prompt");
      return;
    }
    response.status(Status.CREATED_201).send("Prompt inserted successfully");
  }

  /**
   * Handles HTTP PUT requests to update an existing prompt.
   *
   * @param prompt   the prompt to update
   * @param response the server response
   */
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

  /**
   * Handles HTTP DELETE requests to delete a prompt by its ID.
   *
   * @param request  the server request containing the ID of the prompt to delete
   * @param response the server response
   * @throws RestApiException if the prompt ID is not provided
   */
  @DELETE
  @javax.ws.rs.Path("/{id}")
  @Operation(summary = "Delete a prompt by ID", description = "Deletes a prompt by its ID")
  @ApiResponse(content = @Content(mediaType = "text/plain"), responseCode = "200", description = "Successful operation")
  public void deletePromptById(
          @Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the prompt to delete", required = true, schema = @Schema(type = "integer", description = "Chat ID", example = "1"))
          ServerRequest request,
          @Parameter(hidden = true) ServerResponse response
  ) {
    int promptId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new RestApiException("Prompt ID is required"));
    long deletedRows = dbClient.deletePromptById(promptId);
    response.status(Status.OK_200).send("Deleted " + deletedRows + " rows");
  }

  /**
   * Handles HTTP GET requests to retrieve a prompt by its ID.
   *
   * @param request  the server request containing the ID of the prompt to retrieve
   * @param response the server response
   * @throws RestApiException if the prompt ID is not provided
   */
  @GET
  @javax.ws.rs.Path("/{id}")
  @Operation(summary = "Get prompt by ID", description = "Retrieves a prompt by its ID")
  @ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Prompt.class)), responseCode = "200", description = "Successful operation")
  public void getPromptById(
          @Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the prompt to retrieve", required = true, schema = @Schema(type = "integer", description = "Chat ID", example = "1"))
          ServerRequest request,
          @Parameter(hidden = true) ServerResponse response
  ) {
    int promptId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new RestApiException("Prompt ID is required"));
    var prompt = dbClient.getPromptById(promptId);
    response.status(Status.OK_200).send(prompt);
  }

  /**
   * Handles HTTP GET requests to retrieve a list of prompts by chat ID.
   *
   * @param request  the server request containing the chat ID
   * @param response the server response
   * @throws RestApiException if the chat ID is not provided
   */
  @GET
  @javax.ws.rs.Path("/bychat/{id}")
  @Operation(summary = "List prompts by chat ID", description = "Retrieves a list of prompts by chat ID")
  @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = Prompt.class)), mediaType = "application/json"), responseCode = "200", description = "Successful operation")
  public void getPromptsByChatId(
          @Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the chat used to retrieve prompts", required = true, schema = @Schema(type = "integer", description = "Chat ID", example = "1"))
          ServerRequest request,
          @Parameter(hidden = true) ServerResponse response
  ) {
    int chatId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new RestApiException("Chat ID is required"));
    var prompts = dbClient.getPromptsByChatId(chatId);
    response.status(Status.OK_200).send(prompts);
  }

  /**
   * Handles HTTP GET requests to retrieve the first prompt by chat ID.
   *
   * @param request  the server request containing the chat ID
   * @param response the server response
   * @throws RestApiException if the chat ID is not provided
   */
  @GET
  @javax.ws.rs.Path("/bychat/{id}/first")
  @Operation(summary = "Get first prompt by chat ID", description = "Retrieves the first prompt by chat ID")
  @ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Prompt.class)), responseCode = "200", description = "Successful operation")
  public void getFirstPromptByChatId(
          @Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the chat used to retrieve first prompt", required = true, schema = @Schema(type = "integer", description = "Chat ID", example = "1"))
          ServerRequest request,
          @Parameter(hidden = true) ServerResponse response
  ) {
    int chatId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new RestApiException("Chat ID is required"));
    var prompt = dbClient.getFirstPromptByChatId(chatId);
    response.status(Status.OK_200).send(prompt);
  }
}
