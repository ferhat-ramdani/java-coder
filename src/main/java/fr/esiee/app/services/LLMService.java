package fr.esiee.app.services;

import fr.esiee.app.dto.LLMElemDTO;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.http.BadRequestException;
import io.helidon.webserver.http.ServerResponse;

public class LLMService implements HttpService {

  private final DbService dbService;

  public LLMService() {
    dbService = DbService.getInstance();
  }

  @Override
  public void routing(HttpRules httpRules) {
    httpRules.get("/", this::getLLM);
    httpRules.get("/{id}", this::getLLMByid);
  }


  private void getLLM(ServerRequest req, ServerResponse res) {
    var llmToSend = dbService.listLLMs().stream().map(e -> new LLMElemDTO(e.id(),e.name(),e.model(), e.caracteristics())).toList();
    res.status(Status.OK_200).send(llmToSend);
  }

  private void getLLMByid(ServerRequest req, ServerResponse res) {
    int llmId = req.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new BadRequestException("LLM ID is required"));
    var llm = dbService.getLLMById(llmId);
    var llmDTO = new LLMElemDTO(llm.id(),llm.name(),llm.model(), llm.caracteristics());
    res.send(llmDTO);
  }
}
