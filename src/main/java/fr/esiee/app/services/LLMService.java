package fr.esiee.app.services;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import fr.esiee.app.db.entities.Chat;
import fr.esiee.app.dto.LLMElemDTO;
import io.helidon.http.Status;
import io.helidon.webserver.http.*;
import io.helidon.http.BadRequestException;

import java.util.Objects;

public class LLMService implements HttpService {

  private final DbService dbService;
  private final String LLM_BASE_URL = "http://localhost:11434";
  private final String LLM_INSTRUCTS = """
          Respond only with java coce, don't explain, just give the code.
          You must have a main method in your class.
          Whatever text is given by the user, you must generate
          a java class related to the user text.""";

  public LLMService() {
    dbService = DbService.getInstance();
  }

  @Override
  public void routing(HttpRules httpRules) {
    httpRules.get("/", this::getLLM)
            .get("/{id}", this::getLLMByid)
            .post("/class", Handler.create(String.class, this::generateClass));
  }

  private void generateClass(String userRequest, ServerResponse res) {
    Objects.requireNonNull(userRequest);
    var generatedClass = generateClassFromLLM("qwen2.5:0.5b", userRequest);
    System.out.println("Sending generated class ...");
    res.send(generatedClass);
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

  private String generateClassFromLLM(String llmModelName, String requestText) {
    Objects.requireNonNull(llmModelName);
    Objects.requireNonNull(requestText);
    var model = OllamaChatModel.builder()
            .baseUrl(LLM_BASE_URL)
            .modelName(llmModelName)
            .build();
    var answer = model.generate(new SystemMessage(LLM_INSTRUCTS),
            new UserMessage(requestText));
    System.out.println("user request : " + requestText);
    System.out.println("llm answer : " + answer.content().text());
    return answer.content().text();
  }
}
