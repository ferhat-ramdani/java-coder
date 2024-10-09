package fr.esiee.app.services;

import fr.esiee.app.db.entities.Prompt;
import io.helidon.cors.CrossOriginConfig;
import io.helidon.http.NotFoundException;
import io.helidon.http.Status;
import io.helidon.webserver.cors.CorsSupport;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

public class PromptService implements HttpService {

  private final DbService dbClient;

  public PromptService() {
    this.dbClient = DbService.getInstance();
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

  public void listPrompts(ServerRequest request, ServerResponse response) {
    response.send(dbClient.listPrompts());
  }

  public void insertPrompt(Prompt prompt, ServerResponse response) {
    long insertedRows = dbClient.insertPrompt(prompt);
    if (insertedRows <= 0) {
      response.status(Status.BAD_REQUEST_400).send("Failed to insert prompt");
      return;
    }
    response.status(Status.CREATED_201).send("Prompt inserted successfully");
  }

  public void updatePrompt(Prompt prompt, ServerResponse response) {
    long updatedRows = dbClient.updatePrompt(prompt);
    if (updatedRows <= 0) {
      response.status(Status.BAD_REQUEST_400).send("Failed to update prompt");
      return;
    }
    response.status(Status.OK_200).send("Prompt updated successfully");
  }

  public void deletePromptById(ServerRequest request, ServerResponse response) {
    int promptId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new NotFoundException("Prompt ID is required"));
    long deletedRows = dbClient.deletePromptById(promptId);
    response.status(Status.OK_200).send("Deleted " + deletedRows + " rows");
  }

  public void getPromptById(ServerRequest request, ServerResponse response) {
    int promptId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new NotFoundException("Prompt ID is required"));
    var prompt = dbClient.getPromptById(promptId);
    response.send(prompt);
  }

  public void getPromptsByChatId(ServerRequest request, ServerResponse response) {
    int chatId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new NotFoundException("Chat ID is required"));
    var prompts = dbClient.getPromptsByChatId(chatId);
    response.send(prompts);
  }

  public void getFirstPromptByChatId(ServerRequest request, ServerResponse response) {
    int chatId = request.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new NotFoundException("Chat ID is required"));
    var prompt = dbClient.getFirstPromptByChatId(chatId);
    response.send(prompt);
  }
}
