package fr.esiee.app.services;

import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import fr.esiee.app.db.entities.Chat;
import io.helidon.http.BadRequestException;
import io.helidon.http.NotFoundException;
import io.helidon.http.Status;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;

public class ChatService implements HttpService {

  private final DbService dbClient;

  public ChatService() {
    this.dbClient = new DbService();
  }

  @Override
  public void routing(HttpRules httpRules) {
    httpRules.get("/", this::listChats)
            .get("/{id}", this::getChatById)
            .post("/", Handler.create(Chat.class, this::insertChat))
            .put("/", Handler.create(Chat.class, this::updateChat))
            .delete("/{id}", this::deleteChatById);

  }

  private void getChatById(ServerRequest request, ServerResponse response) {
    int chatId = request.path()
            .pathParameters()
            .first("id").map(Integer::parseInt)
            .orElseThrow(() -> new BadRequestException("Chat ID is required"));
    var chat = dbClient.getChatById(chatId);
    response.send(chat);
  }

  @GET
  @javax.ws.rs.Path("/chat")
  @ApiOperation(value = "List all Chats", response = Chat.class, responseContainer = "List")
  public void listChats(ServerRequest request, ServerResponse response) {
    response.send(dbClient.listChats());
  }

  @POST
  @javax.ws.rs.Path("/chat")
  @ApiOperation(value = "Insert a new Chat")
  public void insertChat(Chat chat, ServerResponse response) {
    System.out.println("Inserting chat: " + chat.id());
    long insertedRows = dbClient.insertChat(chat);
    if (insertedRows <= 0) {
      response.status(Status.BAD_REQUEST_400).send("Failed to insert chat");
      return;
    }
    response.status(Status.CREATED_201).send("Inserted " + insertedRows + " rows");
  }

  @PUT
  @javax.ws.rs.Path("/chats")
  @ApiOperation(value = "Update an existing Chat")
  public void updateChat(Chat chat, ServerResponse response) {
    long updatedRows = dbClient.updateChat(chat);
    if (updatedRows <= 0) {
      response.status(Status.BAD_REQUEST_400).send("Failed to update chat");
      return;
    }
    response.status(Status.OK_200).send("Updated " + updatedRows + " rows");
  }

  @DELETE
  @javax.ws.rs.Path("/chats/{id}")
  @ApiOperation(value = "Delete a Chat by ID")
  @ApiResponses(value = {
          @ApiResponse(code = 404, message = "Chat not found")
  })
  public void deleteChatById(ServerRequest request, ServerResponse response) {
    int chatId = request.path()
            .pathParameters()
            .first("id").map(Integer::parseInt)
            .orElseThrow(() -> new BadRequestException("Chat ID is required"));
    var count = dbClient.deleteChatById(chatId);
    response.status(Status.OK_200).send("Deleted " + count + " rows");
  }

}
