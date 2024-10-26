package fr.esiee.app.services;

import fr.esiee.app.db.DbManager;
import fr.esiee.app.db.entities.Chat;
import io.helidon.http.BadRequestException;
import io.helidon.http.Status;
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
@Tag(name = "Chat", description = "endpoints to manipulate chats")
@Path("/api/chat")
public class ChatService implements HttpService {

  private final DbManager dbClient;

  public ChatService() {
    this.dbClient = DbManager.getInstance();
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
  @javax.ws.rs.Path("/{id}")
  @Operation(summary = "Get chat by ID", description = "Retrieves a chat by its ID")
  public void getChatById(
          @Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the chat to retrieve", required = true, schema = @Schema(type = "integer", description = "Chat ID", example = "1")) ServerRequest request,
          @Parameter(hidden = true) ServerResponse response
  ) {
    int chatId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new BadRequestException("Chat ID is required"));
    var chat = dbClient.getChatById(chatId);
    response.send(chat);
  }

  @GET
  @javax.ws.rs.Path("/")
  @Operation(summary = "List all chats", description = "Retrieves a list of all chats")
  public void listChats(
          @Parameter(hidden = true) ServerRequest request,
          @Parameter(hidden = true) ServerResponse response
  ) {
    response.send(dbClient.listChats());
  }

  @POST
  @javax.ws.rs.Path("")
  @Operation(summary = "Insert a chat", description = "Inserts a chat into the database")
  public void insertChat(Chat chat, @Parameter(hidden = true) ServerResponse response) {
    long insertedRows = dbClient.insertChat(chat);
    if (insertedRows <= 0) {
      response.status(Status.BAD_REQUEST_400).send("Failed to insert chat");
      return;
    }
    var latestChat = dbClient.getLatestChat(chat);
    response.status(Status.CREATED_201).send(latestChat);
  }

  @PUT
  @javax.ws.rs.Path("/")
  @Operation(summary = "Update a chat", description = "Updates a chat in the database")
  public void updateChat(Chat chat, @Parameter(hidden = true) ServerResponse response) {
    long updatedRows = dbClient.updateChat(chat);
    if (updatedRows <= 0) {
      response.status(Status.BAD_REQUEST_400).send("Failed to update chat");
      return;
    }
    response.status(Status.OK_200).send("Updated " + updatedRows + " rows");
  }

  @DELETE
  @javax.ws.rs.Path("/{id}")
  @Operation(summary = "Delete a chat by ID", description = "Deletes a chat by its ID")
  public void deleteChatById(
          @Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the chat to delete", schema = @Schema(type = "integer", description = "Chat ID", example = "1")) ServerRequest request,
          @Parameter(hidden = true) ServerResponse response
  ) {
    int chatId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new BadRequestException("Chat ID is required"));
    var count = dbClient.deleteChatById(chatId);
    response.status(Status.OK_200).send("Deleted " + count + " rows");
  }
}
