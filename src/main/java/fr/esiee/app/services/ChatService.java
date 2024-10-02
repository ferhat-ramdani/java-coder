package fr.esiee.app.services;

import fr.esiee.app.db.entities.Chat;
import io.helidon.http.BadRequestException;
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

  public void getChatById(ServerRequest request, ServerResponse response) {
    int chatId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new BadRequestException("Chat ID is required"));
    var chat = dbClient.getChatById(chatId);
    response.send(chat);
  }

  @GET
  @javax.ws.rs.Path("/chat")
  public void listChats(ServerRequest request, ServerResponse response) {
    response.send(dbClient.listChats());
  }

  @POST
  @javax.ws.rs.Path("/chat")
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
  @javax.ws.rs.Path("/chats")
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
  public void deleteChatById(ServerRequest request, ServerResponse response) {
    int chatId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new BadRequestException("Chat ID is required"));
    var count = dbClient.deleteChatById(chatId);
    response.status(Status.OK_200).send("Deleted " + count + " rows");
  }

}
