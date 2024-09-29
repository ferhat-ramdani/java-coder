package fr.esiee.app.services;

import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import fr.esiee.app.db.entities.Prompt;
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

public class PromptService implements HttpService {

  private final DbService dbClient;

  public PromptService() {
    this.dbClient = new DbService();
  }


  @Override
  public void routing(HttpRules httpRules) {
    httpRules.get("/", this::listPrompts)
            .get("/{id}", this::getPromptById)
            .get("/bychat/{id}", this::getPromptsByChatId)
            .post("/", Handler.create(Prompt.class, this::insertPrompt))
            .put("/", Handler.create(Prompt.class, this::updatePrompt))
            .delete("/{id}", this::deletePromptById);
  }

  @GET
  @javax.ws.rs.Path("/")
  @ApiOperation(value = "List all Prompts", response = Prompt.class, responseContainer = "List")
  public void listPrompts(ServerRequest request, ServerResponse response) {
    response.send(dbClient.listPrompts());
  }


  @POST
  @javax.ws.rs.Path("/prompts")
  @ApiOperation(value = "Insert a new Prompt")
  public void insertPrompt(Prompt prompt, ServerResponse response) {
    long insertedRows = dbClient.insertPrompt(prompt);
    if (insertedRows <= 0) {
      response.status(Status.BAD_REQUEST_400).send("Failed to insert prompt");
      return;
    }
    response.status(Status.CREATED_201).send("Prompt inserted successfully");
  }

  @PUT
  @javax.ws.rs.Path("/prompts")
  @ApiOperation(value = "Update an existing Prompt")
  public void updatePrompt(Prompt prompt, ServerResponse response) {
    long updatedRows = dbClient.updatePrompt(prompt);
    if (updatedRows <= 0) {
      response.status(Status.BAD_REQUEST_400).send("Failed to update prompt");
      return;
    }
    response.status(Status.OK_200).send("Prompt updated successfully");
  }

  @DELETE
  @javax.ws.rs.Path("/prompts/{id}")
  @ApiOperation(value = "Delete a Prompt by ID")
  @ApiResponses(value = {
          @ApiResponse(code = 404, message = "Prompt not found")
  })
  public void deletePromptById(ServerRequest request, ServerResponse response) {
    int promptId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new NotFoundException("Prompt ID is required"));
    long deletedRows = dbClient.deletePromptById(promptId);
    response.status(Status.OK_200).send("Deleted " + deletedRows + " rows");
  }

  @GET
  @javax.ws.rs.Path("/prompts/{id}")
  @ApiOperation(value = "Get a Prompt by ID", response = Prompt.class)
  @ApiResponses(value = {
          @ApiResponse(code = 404, message = "Prompt not found")
  })
  public void getPromptById(ServerRequest request, ServerResponse response) {
    int promptId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new NotFoundException("Prompt ID is required"));
    var prompt = dbClient.getPromptById(promptId);
    response.send(prompt);
  }

  @GET
  @javax.ws.rs.Path("/chats/{id}/prompts")
  @ApiOperation(value = "Get Prompts by Chat ID", responseContainer = "List")
  public void getPromptsByChatId(ServerRequest request, ServerResponse response) {
    int chatId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new NotFoundException("Chat ID is required"));
    var prompts = dbClient.getPromptsByChatId(chatId);
    response.send(prompts);
  }
}
