package fr.esiee.app.services;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ollama.OllamaChatModel;
import fr.esiee.app.config.LLMConfig;
import fr.esiee.app.db.entities.AuthorType;
import fr.esiee.app.db.entities.LLM;
import fr.esiee.app.db.entities.Prompt;
import io.helidon.common.context.Contexts;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerResponse;

import java.util.Objects;

public class GeneratorService implements HttpService {

  private final DbService dbService;
  private final LLMConfig llmConfig;

  public GeneratorService() {
    this.dbService = Contexts.globalContext().get(DbService.class).orElse(DbService.getInstance());
    this.llmConfig = Contexts.globalContext().get(LLMConfig.class).orElse(LLMConfig.defaultConfig());
  }

  @Override
  public void routing(HttpRules rules) {
    rules.post("/class", Handler.create(Prompt.class, this::generateClass));
  }

  private void generateClass(Prompt prompt, ServerResponse res) {
    Objects.requireNonNull(prompt);
    var chat = dbService.getChatById(prompt.chatId());
    var llm = dbService.getLLMById(chat.llmId());

    dbService.insertPrompt(prompt);

    var generatedClass = generateClassFromLLM(llm, prompt.message());
    System.out.println("Sending generated class ...");

    var aiPrompt = new Prompt(0, generatedClass, AuthorType.LLM, chat.id());
    dbService.insertPrompt(aiPrompt);

    res.send(generatedClass);
  }

  private String generateClassFromLLM(LLM llm, String requestText) {
    Objects.requireNonNull(llm);
    Objects.requireNonNull(requestText);
    var model = OllamaChatModel.builder().baseUrl(llmConfig.baseUrl()).modelName(llm.model()).temperature(.7d).build();
    var answer = model.generate(new SystemMessage(llm.systemPrompt()), new UserMessage(requestText));
    return answer.content().text();
  }
}
