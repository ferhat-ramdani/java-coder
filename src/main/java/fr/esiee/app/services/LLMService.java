package fr.esiee.app.services;

import fr.esiee.app.config.LLMConfig;
import fr.esiee.app.config.LLMElem;
import fr.esiee.app.dto.LLMElemDTO;
import fr.esiee.app.config.mapper.LLMConfigMapper;
import io.helidon.config.Config;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

public class LLMService implements HttpService {

  private final LLMConfig llms;

  public LLMService() {
    Config config = Config.global();
    this.llms = config.get("llm").as(LLMConfig.class).get();
  }

  @Override
  public void routing(HttpRules httpRules) {
    httpRules.get("/", this::getLLM);
  }


  private void getLLM(ServerRequest req, ServerResponse res) {
    var llmToSend = llms.models().stream().map(e -> new LLMElemDTO(e.name(),e.model())).toList();
    res.status(Status.OK_200).send(llmToSend);
  }
}
