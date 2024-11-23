package fr.esiee.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.esiee.app.db.DbManager;
import fr.esiee.app.db.entities.Chat;
import fr.esiee.app.db.entities.LLM;
import fr.esiee.app.db.entities.Prompt;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbClientException;

import java.io.IOException;
import java.util.List;

public class DbUtils {

  public static List<LLM> llms() throws IOException {
    return List.of(new ObjectMapper().readValue(DbUtils.class.getResourceAsStream("/llms_test.json"),
            LLM[].class));
  }

  public static List<LLM> realLLMs() throws IOException {
    return List.of(new ObjectMapper().readValue(DbUtils.class.getResourceAsStream("/llms.json"),
            LLM[].class));
  }

  public static List<Chat> chats() throws IOException {
    return List.of(new ObjectMapper().readValue(DbUtils.class.getResourceAsStream("/chats.json"), Chat[].class));
  }

  public static List<Prompt> prompts() throws IOException {
    return List.of(new ObjectMapper().readValue(DbUtils.class.getResourceAsStream("/prompts.json"),
            Prompt[].class));
  }

  public static void resetDb() {
    var config = Config.global().get("db");
    try(var dbClient = DbClient.builder(config).build()) {
      dbClient.execute().delete("DROP ALL OBJECTS");
    }
    DbUtils.createSchema();
  }

  public static void createSchema() {
    var config = Config.global().get("db");
    try(var dbClient = DbClient.builder(config).build()) {
      var transaction = dbClient.transaction();
      try {
        transaction.namedDml("create-llm");
        transaction.namedDml("create-chat");
        transaction.namedDml("create-prompt");
        transaction.commit();
      } catch (DbClientException t) {
        transaction.rollback();
        throw t;
      }
    }
  }

  public static void initializeLLM() throws IOException {
    var config = Config.global().get("db");
    try(var dbClient = DbClient.builder(config).build()) {
      for (var llm : llms()) {
        dbClient.execute().createInsert(
                        "INSERT INTO llm(id, name, model, system_prompt, characteristics, temp, seed, timeout_sec) VALUES(?, ?, ?, ?, ?, ?, ?, ?)")
                .addParam(llm.id()).addParam(llm.name()).addParam(llm.model()).addParam(llm.systemPrompt())
                .addParam(llm.characteristics()).addParam(llm.temp()).addParam(llm.seed()).addParam(llm.timeoutSec()).execute();
      }
    }
  }

  public static void initializeRealLLM() throws IOException {
    var config = Config.global().get("db");
    try(var dbClient = DbClient.builder(config).build()) {
      var tx = dbClient.transaction();
      var llms = new ObjectMapper().readTree(DbManager.class.getResourceAsStream("/llms.json"));
      for (var llm : llms) {
        tx.namedInsert("insert-llm",
                llm.get("name").asText(),
                llm.get("model").asText(),
                llm.get("system_prompt").asText(""),
                llm.get("characteristics").asText(""),
                llm.get("temp").asDouble(),
                llm.get("seed").asInt(),
                llm.get("timeout_sec").asInt());
      }
      tx.commit();
    }
  }

  public static void initializeChats() throws IOException {
    var config = Config.global().get("db");
    try(var dbClient = DbClient.builder(config).build()) {
      for (var chat : chats()) {
        dbClient.execute().createInsert("INSERT INTO Chat(id, title, last_activity, llm_id) VALUES(?, ?, ?, ?)")
                .addParam(chat.id()).addParam(chat.title()).addParam(chat.lastActivity()).addParam(chat.llmId())
                .execute();
      }
    }
  }

  public static void initializePrompts() throws IOException {
    var config = Config.global().get("db");
    try(var dbClient = DbClient.builder(config).build()) {
      for (var prompt : prompts()) {
        dbClient.execute()
                .createInsert("INSERT INTO Prompt(id, message, author_type, chat_id, compile) VALUES(?, ?, ?, ?, ?)")
                .addParam(prompt.id()).addParam(prompt.message()).addParam(prompt.authorType().name())
                .addParam(prompt.chatId()).addParam(prompt.compile()).execute();
      }
    }
  }


}
