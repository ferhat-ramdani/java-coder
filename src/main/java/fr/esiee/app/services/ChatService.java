package fr.esiee.app.services;

import fr.esiee.app.db.DbManager;
import fr.esiee.app.db.entities.Chat;
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
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.util.NoSuchElementException;

@Tag(name = "Chat", description = "endpoints to manipulate chats")
@Path("/api/chat")
public class ChatService implements HttpService {

  private final DbManager dbClient;

  public ChatService() {
    this.dbClient = Contexts.globalContext().get(DbManager.class).orElseThrow(() -> new NoSuchElementException("DbManager not found."));
  }

  @Override
  public void routing(HttpRules httpRules) {
    httpRules.get("/", this::listChats)

            .get("/{id}", this::getChatById)
            .post("/", Handler.create(Chat.class, this::insertChat))
            .put("/", Handler.create(Chat.class, this::updateChat))
            .delete("/{id}", this::deleteChatById);

  }

  @GET
  @Path("/{id}")
  @Operation(summary = "Get chat by ID", description = "Retrieves a chat by its ID")
  @ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Chat.class)), responseCode = "200", description = "Successful operation")
  public void getChatById(
          @Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the chat to retrieve", required = true, schema = @Schema(type = "integer", description = "Chat ID", example = "1")) ServerRequest request,
          @Parameter(hidden = true) ServerResponse response
  ) {
    int chatId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new RestApiException("Chat ID is required"));
    var chat = dbClient.getChatById(chatId);
    response.status(Status.OK_200).send(chat);
  }

  @GET
  @Path("/")
  @Operation(summary = "List all chats", description = "Retrieves a list of all chats")
  @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = Chat.class)), mediaType = "application/json"), responseCode = "200", description = "Successful operation")
  public void listChats(
          @Parameter(hidden = true) ServerRequest request,
          @Parameter(hidden = true) ServerResponse response
  ) {
    response.status(Status.OK_200).send(dbClient.listChats());
  }

  @POST
  @Path("/")
  @Operation(summary = "Insert a chat", description = "Inserts a chat into the database")
  @Consumes("application/json")
  @ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Chat.class)), responseCode = "201", description = "Chat inserted successfully")
  public void insertChat(@RequestBody(required = true, content = @Content(schema = @Schema(implementation = Chat.class))) Chat chat, @Parameter(hidden = true) ServerResponse response) {
    long insertedRows = dbClient.insertChat(chat);
    if (insertedRows <= 0) {
      ErrorUtils.send(response, Status.BAD_REQUEST_400, "Failed to insert chat");
      return;
    }
    var latestChat = dbClient.getLatestChat(chat);
    response.status(Status.CREATED_201).send(latestChat);
  }

  @PUT
  @Path("/")
  @Operation(summary = "Update a chat", description = "Updates a chat in the database")
  @Consumes("application/json")
  @ApiResponse(content = @Content(mediaType = "text/plain"), responseCode = "200", description = "Successful operation")
  public void updateChat(@RequestBody(required = true, content = @Content(schema = @Schema(implementation = Chat.class))) Chat chat, @Parameter(hidden = true) ServerResponse response) {
    long updatedRows = dbClient.updateChat(chat);
    if (updatedRows <= 0) {
      ErrorUtils.send(response, Status.BAD_REQUEST_400, "Failed to update chat");
      return;
    }
    response.status(Status.OK_200).send("Updated " + updatedRows + " rows");
  }

  @DELETE
  @Path("/{id}")
  @Operation(summary = "Delete a chat by ID", description = "Deletes a chat by its ID")
  @ApiResponse(content = @Content(mediaType = "text/plain"), responseCode = "200", description = "Successful operation")
  public void deleteChatById(
          @Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the chat to delete", schema = @Schema(type = "integer", description = "Chat ID", example = "1")) ServerRequest request,
          @Parameter(hidden = true) ServerResponse response
  ) {
    int chatId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new RestApiException("Chat ID is required"));
    var count = dbClient.deleteChatById(chatId);
    response.status(Status.OK_200).send("Deleted " + count + " rows");
  }
}
