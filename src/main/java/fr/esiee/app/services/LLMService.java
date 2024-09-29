package fr.esiee.app.services;

import fr.esiee.app.dto.LLMElemDTO;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

public class LLMService implements HttpService {

  private final DbService dbService;

  public LLMService() {
    dbService = new DbService();
  }

  @Override
  public void routing(HttpRules httpRules) {
    httpRules.get("/", this::getLLM);
  }


  private void getLLM(ServerRequest req, ServerResponse res) {
    var llmToSend = dbService.listLLMs().stream().map(e -> new LLMElemDTO(e.id(),e.name(),e.model())).toList();
    res.status(Status.OK_200).send(llmToSend);
  }
}
