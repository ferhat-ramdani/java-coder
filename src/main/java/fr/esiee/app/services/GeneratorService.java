package fr.esiee.app.services;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import fr.esiee.app.config.LLMProviderConfig;
import fr.esiee.app.db.entities.AuthorType;
import fr.esiee.app.db.entities.Chat;
import fr.esiee.app.db.entities.LLM;
import fr.esiee.app.db.entities.Prompt;
import fr.esiee.app.dto.Assistant;
import io.helidon.common.context.Contexts;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

public class GeneratorService implements HttpService {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorService.class);
  private static final String SYSTEM_ERR_MESSAGE = "There are compilation errors in the generated class : ";
  private static final String USER_ERR_MESSAGE = "There are compilation errors in the generated class : ";
  private final DbService dbService;
  private final LLMProviderConfig llmConfig;
  private final int NB_ATTEMPTS = 5;
  private final int MAX_MEMORY_PROMPTS = 300;

  private ChatMemory chatMemory;
  private LLM curLLM;
  private OllamaChatModel model;
  private Assistant assistant;

  public GeneratorService() {
    this.dbService = Contexts.globalContext().get(DbService.class).orElse(DbService.getInstance());
    this.llmConfig = Contexts.globalContext().get(LLMProviderConfig.class).orElse(LLMProviderConfig.defaultConfig());
  }

  @Override
  public void routing(HttpRules rules) {
    rules.post("/class", Handler.create(Prompt.class, this::generateClass));
  }

  private void generateClass(Prompt prompt, ServerResponse res) {
    Objects.requireNonNull(prompt);
    var chat = dbService.getChatById(prompt.chatId());
    var newLLM = dbService.getLLMById(chat.llmId());
    dbService.insertPrompt(prompt);
    LOGGER.info("User request : {}", prompt.message());

    String generatedClass = "Error generating class.";
    try {
      generatedClass = generateClassFromLLM(newLLM, prompt.message(), chat);
      LOGGER.info("Generated class after compilation : {}", generatedClass);
    } catch (IOException | RuntimeException e) {
      LOGGER.error("Error while generating class : {}", e.getMessage());
    }

    var aiPrompt = new Prompt(0, generatedClass, AuthorType.LLM, chat.id());
    dbService.insertPrompt(aiPrompt);
    res.send(generatedClass);
  }

  private String generateClassFromLLM(LLM llm, String requestText, Chat chat) throws IOException {
    Objects.requireNonNull(llm);
    Objects.requireNonNull(requestText);
    Objects.requireNonNull(chat);
    if (llm != curLLM || model == null || assistant == null) {
      updateModelSettings(llm);
      curLLM = llm;
    }

    String errorsText = null;
    for (int attempt = 0; attempt < NB_ATTEMPTS; attempt++) {
      LOGGER.info("Attempting to generate class : {}", attempt);
      var answer = assistant.chat(attempt == 0 ? llm.systemPrompt() : errorsText);
      LOGGER.info("Generated class before compilation : {}", answer);
      String code = CompileService.extractCode(answer);
      LOGGER.info("Extracted code : {}", code);
      var errors = CompileService.processAndCompileText(code);
      LOGGER.info("Compilation message : {}", errors);
      if (errors.isEmpty()) {
        return code;
      } else {
        errorsText = SYSTEM_ERR_MESSAGE + "\n" + String.join("\n\n", errors);
      }
    }

    return USER_ERR_MESSAGE;
  }

  private void updateModelSettings(LLM llm) {
    chatMemory = MessageWindowChatMemory.withMaxMessages(MAX_MEMORY_PROMPTS);
    model = OllamaChatModel.builder()
            .baseUrl(llmConfig.baseUrl())
            .modelName(llm.model())
            .temperature(.7d)
            .build();
    assistant = AiServices.builder(Assistant.class)
            .chatLanguageModel(model)
            .chatMemory(chatMemory)
            .build();
  }
}
