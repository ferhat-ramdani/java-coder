package fr.esiee.app.services;

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
import io.swagger.v3.oas.annotations.info.Info;
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
@Tag(name = "Chat")
@Path("/api/chat")
public class ChatService implements HttpService {

  private final DbService dbClient;

  public ChatService() {
    this.dbClient = DbService.getInstance();
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
  @Operation(summary = "Get chat by id",
          description = "Retrieve a chat by its id")
  public void getChatById(ServerRequest request, ServerResponse response) {
    int chatId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new BadRequestException("Chat ID is required"));
    var chat = dbClient.getChatById(chatId);
    response.send(chat);
  }

  @GET
  @javax.ws.rs.Path("/")
  @Operation(summary = "List chats",
          description = "List of all the chats")
  public void listChats(ServerRequest request, ServerResponse response) {
    response.send(dbClient.listChats());
  }

  @POST
  @javax.ws.rs.Path("/")
  @Operation(summary = "insert a chat",
          description = "adds a chat to the chat table")
  public void insertChat(Chat chat, ServerResponse response) {
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
  @Operation(summary = "Update a chat",
          description = "Updates a chat in the chats table")
  public void updateChat(@Parameter(description = "The chat to update", required = true)
                           Chat chat, ServerResponse response) {
    long updatedRows = dbClient.updateChat(chat);
    if (updatedRows <= 0) {
      response.status(Status.BAD_REQUEST_400).send("Failed to update chat");
      return;
    }
    response.status(Status.OK_200).send("Updated " + updatedRows + " rows");
  }

  @DELETE
  @javax.ws.rs.Path("/{id}")
  @Operation(summary = "delete a chat by id",
          description = "remove a chat from chat table by its id")
  public void deleteChatById(ServerRequest request, ServerResponse response) {
    int chatId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new BadRequestException("Chat ID is required"));
    var count = dbClient.deleteChatById(chatId);
    response.status(Status.OK_200).send("Deleted " + count + " rows");
  }
}
