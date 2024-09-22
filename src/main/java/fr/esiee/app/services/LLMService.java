package fr.esiee.app.services;

import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

public class LLMService implements HttpService {
  @Override
  public void routing(HttpRules httpRules) {
    httpRules.get("/", this::getLLM);
  }


  private void getLLM(ServerRequest req, ServerResponse res) {
    res.send("LLM Service");

  }
}
