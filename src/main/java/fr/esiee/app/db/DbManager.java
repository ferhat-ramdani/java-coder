package fr.esiee.app.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.esiee.app.db.entities.Chat;
import fr.esiee.app.db.entities.LLM;
import fr.esiee.app.db.entities.Prompt;
import io.helidon.common.context.Contexts;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbClientException;
import io.helidon.dbclient.DbExecute;
import io.helidon.http.NotFoundException;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Manages the database operations for the application.
 */
public class DbManager {

  // We don't have injection, so we need to use this declaration.
  private static final Logger LOGGER = LoggerFactory.getLogger(DbManager.class);

  private final DbClient dbClient;

  /**
   * Only used for testing purposes.
   * <p>
   * Creates a new DbManager instance with the specified DbClient.
   *
   * @param dbClient the DbClient instance to use for database operations
   */
  @TestOnly
  DbManager(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  /**
   * Creates a new DbManager instance.
   *
   * @throws IOException if an I/O error occurs during initialization
   */
  private DbManager() throws IOException {
    var config = Config.global().get("db");
    this.dbClient = Contexts.globalContext()
            .get(DbClient.class)
            .orElseGet(() -> DbClient.create(config));

    Contexts.globalContext().register(dbClient);

    setupSchema();

    if (getLLMCount() <= 0) {
      setupData();
    }
  }

  /**
   * Truncates the given string to the specified length.
   *
   * @param str    the string to be truncated
   * @param length the maximum length of the truncated string
   * @return the truncated string if its length exceeds the specified length, otherwise the original string
   */
  static String truncate(String str, int length) {
    return str.length() > length ? str.substring(0, length) : str;
  }

  /**
   * Initializes the DbManager instance.
   *
   * @throws IOException if an I/O error occurs during initialization.
   */
  public static void initialize() throws IOException {
    if (Contexts.globalContext().get(DbManager.class).isPresent()) {
      LOGGER.info("DbManager already initialized");
      return;
    }

    var config = Config.global();
    var dbUser = config.get("db.connection.username").asString()
            .orElseThrow(() -> new RuntimeException("Database username is not set."));
    var dbPassword = config.get("db.connection.password").asString()
            .orElseThrow(() -> new RuntimeException("Database password is not set."));

    if (dbUser.isBlank()) {
      LOGGER.error("Database username is not set.");
    }

    if (dbPassword.isBlank()) {
      LOGGER.error("Database password is not set.");
    }

    if (dbUser.equals("gptfordev") || dbPassword.equals("gptfordev")) {
      LOGGER.warn("You are using the default database user or password.");
      LOGGER.warn("To change it, you can set the values in the `application.yaml` file.");
      LOGGER.warn(
              "Or in the environment variables. By set `db_connection_username` and `db_connection_password` values.");
    }

    var dbManager = new DbManager();
    Contexts.globalContext().register(dbManager);
  }

  /**
   * Inserts LLM data into the database.
   *
   * @param exec the DbExecute instance used to execute the insert statements
   * @throws IOException if an I/O error occurs while reading the LLM data
   */
  private static void setupLLMs(DbExecute exec) throws IOException {
    var llms = new ObjectMapper().readTree(DbManager.class.getResourceAsStream("/llms.json"));
    for (var llm : llms) {
      exec.namedInsert("insert-llm",
              llm.get("name").asText(),
              llm.get("model").asText(),
              llm.get("system_prompt").asText(""),
              llm.get("characteristics").asText(""),
              llm.get("temp").asDouble(),
              llm.get("seed").asInt(),
              llm.get("timeout_sec").asInt());
    }
  }

  /**
   * Sets up the database schema by creating the necessary tables.
   * This method uses a transaction to ensure that all tables are created
   * successfully. If any table creation fails, the transaction is rolled back.
   *
   * @throws DbClientException if there is an error during the table creation process.
   */
  void setupSchema() {
    var transaction = dbClient.transaction();
    try {
      transaction.namedDml("create-llm");
      transaction.namedDml("create-chat");
      transaction.namedDml("create-prompt");
      transaction.commit();
    } catch (DbClientException t) {
      LOGGER.warn("Could not create tables");
      transaction.rollback();
      throw t;
    }
  }

  /**
   * Sets up the initial data in the database.
   * This method inserts LLM data into the database using a transaction.
   *
   * @throws IOException if an I/O error occurs while reading the LLM data.
   */
  void setupData() throws IOException {
    var tx = dbClient.transaction();
    try {
      setupLLMs(tx);
      tx.commit();
    } catch (DbClientException | IOException t) {
      tx.rollback();
      throw t;
    }
  }

  /**
   * Retrieves a list of all chats from the database.
   *
   * @return a list of Chat objects representing all chats in the database.
   */
  public List<Chat> listChats() {
    return dbClient.execute()
            .namedQuery("select-all-chats")
            .map(e -> e.as(Chat.class)).toList();
  }

  /**
   * Retrieves a list of all prompts from the database.
   *
   * @return a list of Prompt objects representing all prompts in the database.
   */
  public List<Prompt> listPrompts() {
    return dbClient.execute()
            .namedQuery("select-all-prompts")
            .map(e -> e.as(Prompt.class))
            .toList();
  }

  /**
   * Retrieves a prompts from the database by id.
   *
   * @return a Prompt object representing the prompt in the database.
   */
  public Prompt getPromptById(int promptId) {
    return dbClient.execute()
            .createNamedGet("select-prompt-by-id")
            .addParam("id", promptId)
            .execute()
            .orElseThrow(() -> {
              LOGGER.error("Prompt {} not found", promptId);
              return new NotFoundException("Prompt " + promptId + " not found");
            })
            .as(Prompt.class);
  }


  /**
   * Retrieves the count of all LLMs in the database.
   *
   * @return the number of LLMs in the database.
   */
  private int getLLMCount() {
    return dbClient.execute()
            .namedQuery("select-all-llms")
            .map(e -> e.as(LLM.class))
            .toList().size();
  }

  /**
   * Inserts a new prompt into the database.
   *
   * @param prompt the Prompt object to be inserted
   * @return the number of rows updated
   * @throws IllegalArgumentException if the prompt id or chatId is negative, or if the prompt already exists
   * @throws DbClientException        if there is an error during the database operation
   * @throws BadRequestException      if the prompt insertion fails
   */
  public long insertPrompt(Prompt prompt) {
    if (prompt.id() < 0 || prompt.chatId() < 0) {
      throw new IllegalArgumentException("id, llmId, or chatId is negative");
    }
    var transaction = dbClient.transaction();
    long updatedRows;
    try {
      updatedRows = transaction.createNamedInsert("insert-prompt")
              .addParam(prompt.message())
              .addParam(prompt.authorType().name())
              .addParam(prompt.chatId())
              .addParam(prompt.compile())
              .execute();
      transaction.commit();
    } catch (DbClientException t) {
      transaction.rollback();
      throw t;
    }
    if (updatedRows <= 0) {
      throw new BadRequestException("Failed to insert prompt");
    }
    return updatedRows;
  }

  /**
   * Updates the last activity timestamp of a chat.
   *
   * @param chatId the ID of the chat to update
   * @throws DbClientException if there is an error during the database operation
   */
  public void updateChatLastActivity(int chatId) {
    var transaction = dbClient.transaction();
    try {
      transaction.createNamedUpdate("update-chat-last-activity")
              .addParam("id", chatId)
              .addParam("lastActivity", Timestamp.from(Instant.now()))
              .execute();
      transaction.commit();
    } catch (DbClientException t) {
      transaction.rollback();
      throw t;
    }
  }

  /**
   * Updates an existing prompt in the database.
   *
   * @param prompt the Prompt object containing updated information
   * @return the number of rows updated
   * @throws IllegalArgumentException if the prompt id or chatId is negative
   * @throws NotFoundException        if the prompt does not exist
   * @throws DbClientException        if there is an error during the database operation
   */
  public long updatePrompt(Prompt prompt) {
    if (prompt.id() < 0 || prompt.chatId() < 0) {
      throw new IllegalArgumentException("id, llmId, or chatId is negative");
    }
    if (!promptExists(prompt.id())) {
      throw new NotFoundException("Prompt " + prompt.id() + " not found");
    }

    var transaction = dbClient.transaction();
    long updatedRow;
    try {
      updatedRow = transaction.createNamedUpdate("update-prompt-by-id")
              .addParam("message", prompt.message())
              .addParam("authorType", prompt.authorType().name())
              .addParam("chatId", prompt.chatId())
              .addParam("compile", prompt.compile())
              .addParam("id", prompt.id())
              .execute();
      transaction.commit();
    } catch (DbClientException t) {
      transaction.rollback();
      throw t;
    }
    return updatedRow;
  }

  /**
   * Deletes a prompt from the database by its ID.
   *
   * @param promptId the ID of the prompt to be deleted
   * @return the number of rows affected by the delete operation
   * @throws NotFoundException if the prompt with the specified ID is not found
   * @throws DbClientException if there is an error during the database operation
   */
  public long deletePromptById(int promptId) {
    var transaction = dbClient.transaction();
    long count;
    try {
      count = transaction.createNamedDelete("delete-prompt-by-id")
              .addParam("id", promptId)
              .execute();

      transaction.commit();
    } catch (DbClientException t) {
      transaction.rollback();
      throw t;
    }

    if (count == 0) {
      throw new NotFoundException("Prompt " + promptId + " not found");
    }
    return count;
  }

  /**
   * Retrieves a list of prompts associated with a specific chat ID.
   *
   * @param promptId the ID of the chat for which to retrieve prompts
   * @return a list of Prompt objects associated with the specified chat ID
   */
  public List<Prompt> getPromptsByChatId(int promptId) {
    return dbClient.execute()
            .createNamedQuery("select-prompts-by-chat-id")
            .addParam("chatId", promptId)
            .execute()
            .map(e -> e.as(Prompt.class))
            .toList();
  }

  /**
   * Inserts a new chat into the database.
   *
   * @param chat the Chat object to be inserted
   * @return the number of rows updated
   * @throws IllegalArgumentException if the chat id or llmId is negative
   * @throws DbClientException        if there is an error during the database operation
   */
  public long insertChat(Chat chat) {
    if (chat.id() < 0 || chat.llmId() < 0) {
      throw new IllegalArgumentException("id or llmId is negative");
    }
    var transaction = dbClient.transaction();

    long updatedRow;
    try {
      updatedRow = transaction.createNamedInsert("insert-chat")
              .addParam(truncate(chat.title(), 100))
              .addParam(chat.lastActivity())
              .addParam(chat.llmId()).execute();
      transaction.commit();
    } catch (DbClientException t) {
      LOGGER.error("Exception occurred while trying to insert a chat to database : ", t);
      transaction.rollback();
      throw t;
    }

    return updatedRow;
  }

  /**
   * Retrieves a chat from the database based on the provided chat parameters.
   *
   * @param chat the Chat object containing the parameters to search for
   * @return the Chat object matching the provided parameters
   * @throws NotFoundException if no chat matching the provided parameters is found
   */
  public Chat getChatByParams(Chat chat) {
    return dbClient.execute()
            .createNamedGet("select-chat-by-params")
            .addParam("title", truncate(chat.title(), 100))
            .addParam("last_activity", chat.lastActivity())
            .addParam("llm_id", chat.llmId())
            .execute()
            .orElseThrow(() -> new NotFoundException("Chat " + chat + " not found"))
            .as(Chat.class);
  }

  /**
   * Retrieves a prompt from the database based on the provided prompt information.
   *
   * @param prompt the Prompt object containing the parameters to search for
   * @return the Prompt object matching the provided parameters
   * @throws NotFoundException if no prompt matching the provided parameters is found
   */
  public Prompt getPromptByPromptInfo(Prompt prompt) {
    return dbClient.execute()
            .createNamedGet("get-prompt-by-prompt-info")
            .addParam("message", prompt.message())
            .addParam("authorType", prompt.authorType().name())
            .addParam("chatId", prompt.chatId())
            .addParam("compile", prompt.compile())
            .execute()
            .orElseThrow(() -> new NotFoundException("Prompt not found"))
            .as(Prompt.class);
  }

  /**
   * Checks if a chat with the specified ID exists in the database.
   *
   * @param chatId the ID of the chat to check for existence
   * @return true if the chat exists, false otherwise
   */
  public boolean chatExists(int chatId) {
    return dbClient.execute()
            .createNamedGet("select-chat-by-id")
            .addParam("id", chatId)
            .execute()
            .isPresent();
  }

  /**
   * Checks if a prompt with the specified ID exists in the database.
   *
   * @param promptId the ID of the prompt to check for existence
   * @return true if the prompt exists, false otherwise
   */
  public boolean promptExists(int promptId) {
    return dbClient.execute()
            .createNamedGet("select-prompt-by-id")
            .addParam("id", promptId)
            .execute()
            .isPresent();
  }

  /**
   * Retrieves a chat from the database by its ID.
   *
   * @param chatId the ID of the chat to retrieve
   * @return the Chat object corresponding to the specified ID
   * @throws NotFoundException if the chat with the specified ID is not found
   */
  public Chat getChatById(int chatId) {
    return dbClient.execute()
            .createNamedGet("select-chat-by-id")
            .addParam("id", chatId)
            .execute()
            .orElseThrow(() -> new NotFoundException("Chat " + chatId + " not found"))
            .as(Chat.class);
  }

  /**
   * Updates an existing chat in the database.
   *
   * @param chat the Chat object containing updated information
   * @return the number of rows updated
   * @throws IllegalArgumentException if the chat id or llmId is negative
   * @throws NotFoundException        if the chat does not exist
   * @throws DbClientException        if there is an error during the database operation
   */
  public long updateChat(Chat chat) {
    if (chat.id() < 0 || chat.llmId() < 0) {
      throw new IllegalArgumentException("id or llmId is negative");
    }
    if (!chatExists(chat.id())) {
      throw new NotFoundException("Chat " + chat.id() + " not found");
    }

    var transaction = dbClient.transaction();
    long updatedRow;

    var newChat = new Chat(chat.id(), truncate(chat.title(), 100), chat.lastActivity(), chat.llmId());
    try {
      updatedRow = transaction.createNamedUpdate("update-chat-by-id")
              .namedParam(newChat).execute();
      transaction.commit();
    } catch (DbClientException t) {
      transaction.rollback();
      throw t;
    }
    return updatedRow;
  }

  /**
   * Deletes a chat from the database by its ID.
   * Note that this method does not delete the prompts associated with the chat.
   *
   * @param chatId the ID of the chat to be deleted
   * @return the number of rows affected by the delete operation
   * @throws NotFoundException if the chat with the specified ID is not found
   * @throws DbClientException if there is an error during the database operation
   */
  public long deleteChatById(int chatId) {
    var transaction = dbClient.transaction();

    long count;
    try {
      count = transaction.createNamedDelete("delete-chat-by-id")
              .addParam("id", chatId)
              .execute();
      transaction.commit();
    } catch (DbClientException t) {
      transaction.rollback();
      throw t;
    }

    if (count == 0) {
      throw new NotFoundException("Chat.ts " + chatId + " not found");
    }
    return count;
  }

  /**
   * Retrieves a list of all LLMs from the database.
   *
   * @return a list of LLM objects representing all LLMs in the database.
   */
  public List<LLM> listLLMs() {
    return dbClient.execute()
            .namedQuery("select-all-llms")
            .map(e -> e.as(LLM.class))
            .toList();
  }

  /**
   * Inserts a new LLM into the database.
   *
   * @param llm the LLM object to be inserted
   * @return the number of rows updated
   * @throws DbClientException if there is an error during the database operation
   */
  public long insertLLM(LLM llm) {
    var transaction = dbClient.transaction();
    long updatedRow;
    try {
      updatedRow = transaction.createNamedInsert("insert-llm")
              .addParam(llm.name())
              .addParam(llm.model())
              .addParam(llm.systemPrompt())
              .addParam(llm.characteristics())
              .addParam(llm.temp())
              .addParam(llm.seed())
              .addParam(llm.timeoutSec())
              .execute();
      transaction.commit();
    } catch (DbClientException t) {
      LOGGER.error("Exception occurred while trying to insert a LLM to database : ", t);
      transaction.rollback();
      throw t;
    }
    return updatedRow;
  }

  /**
   * Retrieves the first LLM from the database.
   *
   * @return the first LLM object found in the database
   * @throws NotFoundException if no LLM is found in the database
   */
  public LLM getFirstLLM() {
    return dbClient.execute()
            .createNamedGet("get-first-llm")
            .execute()
            .orElseThrow(() -> new NotFoundException("LLM not found"))
            .as(LLM.class);
  }

  /**
   * Retrieves an LLM from the database by its ID.
   *
   * @param llmId the ID of the LLM to retrieve
   * @return the LLM object corresponding to the specified ID
   * @throws NotFoundException if the LLM with the specified ID is not found
   */
  public LLM getLLMById(int llmId) {
    return dbClient.execute()
            .createNamedGet("select-llm-by-id")
            .addParam("id", llmId)
            .execute()
            .orElseThrow(() -> new NotFoundException("LLM " + llmId + " not found"))
            .as(LLM.class);
  }

  /**
   * Retrieves the first prompt associated with a specific chat ID.
   *
   * @param chatId the ID of the chat for which to retrieve the first prompt
   * @return the first Prompt object associated with the specified chat ID
   * @throws NotFoundException if no prompt is found for the specified chat ID
   */
  public Prompt getFirstPromptByChatId(int chatId) {
    return dbClient.execute()
            .createNamedQuery("select-first-prompt-by-chat-id")
            .addParam("chatId", chatId)
            .execute()
            .map(e -> e.as(Prompt.class))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Chat " + chatId + " not found"));
  }
}
