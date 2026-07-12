package fr.esiee.app.llms;

import fr.esiee.app.config.LLMConfig;
import fr.esiee.app.db.DbManager;
import io.helidon.common.context.Contexts;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;

/**
 * A class that manages the setup of Ollama and LLMs (Language Learning Models).
 */
public class OllamaSetupManager {

  // We don't have injection, so we need to use this declaration.
  private static final Logger LOGGER = LoggerFactory.getLogger(OllamaSetupManager.class);

  /**
   * Record to hold the configuration for Ollama.
   *
   * @param ollamaPath the path where Ollama is installed
   * @param file the file name of the Ollama package
   * @param extractCmd the command to extract the Ollama package
   * @param cmdPrefix the prefix for the command to execute
   * @param cmd the command to execute
   */
  private record Config(Path ollamaPath, String file, String extractCmd, String cmdPrefix, String cmd, String killCmd) {

    /**
     * Adjusts the command to include the command prefix and the resolved path to the Ollama.
     *
     * @param cmd the command to adjust
     * @return the adjusted command string
     */
    public String adjustCmd(String cmd) {
      return cmdPrefix + ollamaPath.resolve(this.cmd) + " " + cmd;
    }
  }

  /**
   * Sets up and starts the Ollama service.
   *
   * This method performs the following steps:
   * 1. Retrieves the configuration for Ollama.
   * 2. Checks if Ollama is installed. If not, it installs Ollama.
   * 3. Starts the Ollama service.
   * 4. Pulls the LLMs and ensures they are ready.
   *
   * @throws IOException if an I/O error occurs during the setup process.
   * @throws InterruptedException if the current thread is interrupted while waiting.
   */
  public static void setupAndStartOllama() throws IOException, InterruptedException {
    Config config = getConfig();

    if (!IsOllamaInstalled(config)) {
      LOGGER.info("Installing Ollama...");
      if(!installOllama(config)) {
        LOGGER.error("Error installing Ollama.");
        throw new UnsupportedOperationException("Error installing Ollama.");
      }
    } else {
      LOGGER.info("Ollama is already installed.");
    }

    if(startOllama(config) && pullLLMS(config)) {
      LOGGER.info("Ollama and LLMs are ready.");
    } else {
      LOGGER.error("Error setting up Ollama and LLMs.");
      throw new UnsupportedOperationException("Error setting up Ollama and LLMs.");
    }
  }

  /**
   * Retrieves the configuration for Ollama based on the operating system.
   *
   * @return the configuration for Ollama
   * @throws UnsupportedOperationException if the operating system is not supported
   */
  private static Config getConfig() {
    var localPath = Path.of(SystemUtils.USER_HOME, ".javacoder");
    if (SystemUtils.IS_OS_LINUX) {
      return new Config(localPath, "ollama-linux-" + SystemUtils.OS_ARCH + ".tgz", "tar -xzf %src% -C %dest%", "", "bin/ollama", "killall ollama");
    } else if (SystemUtils.IS_OS_WINDOWS) {
      return new Config(localPath, "ollama-windows-" + SystemUtils.OS_ARCH + ".zip", "powershell -command \"Expand-Archive -Path '%src%' -DestinationPath '%dest%' -Force\"", "cmd /c ", "ollama.exe", "taskkill /F /IM ollama.exe");
    } else if (SystemUtils.IS_OS_MAC) {
      return new Config(localPath, "ollama-darwin", "mv %src% %dest%", "", "ollama", "killall ollama");
    } else {
      LOGGER.error("Unsupported OS: {}", SystemUtils.OS_NAME);
      throw new UnsupportedOperationException("Unsupported OS: " + SystemUtils.OS_NAME);
    }
  }


  /**
   * Starts the Ollama service.
   *
   * @param config the configuration for Ollama
   * @return true if Ollama started successfully, false otherwise
   * @throws IOException if an I/O error occurs
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  private static boolean startOllama(Config config) throws IOException, InterruptedException {
    LOGGER.info("Starting Ollama...");
    var sucess = executeCMD(config.adjustCmd("serve"), CMDType.RUN_NO_WAIT, false, config);
    if (sucess) {
      LOGGER.info("Ollama started successfully.");
    } else {
      LOGGER.error("Error starting Ollama.");
    }
    return sucess;
  }

  /**
   * Pulls the LLMs from the database and ensures they are present.
   *
   * @param config the configuration for Ollama
   * @return true if all LLMs are successfully pulled or already present, false otherwise
   * @throws IOException if an I/O error occurs
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  private static boolean pullLLMS(Config config) throws IOException, InterruptedException {
    var llmList = Contexts.globalContext()
            .get(DbManager.class)
            .orElseThrow(() -> new NoSuchElementException("DbManager not found."))
            .listLLMs();
    LOGGER.info("Waiting for {} LLMs to be pulled.", llmList.size());

    for (int i = 0; i < llmList.size(); i++) {
      var llm = llmList.get(i);
      LOGGER.info("Checking if LLM is present: {} - {}/{}", llm.model(), i + 1, llmList.size());
      if (!isLLMPresent(config, llm.model())) {
        LOGGER.info("Pulling LLM: {}", llm.model());
        if (!executeCMD(config.adjustCmd("pull " + llm.model()), CMDType.RUN, true, config)) {
          LOGGER.error("Error pulling LLM: {}", llm.model());
          return false;
        }
      } else {
        LOGGER.info("LLM {} is already present. Skipping...", llm.model());
      }
    }
    LOGGER.info("LLMs checked.");
    return true;
  }

  /**
   * Pulls a single LLM and ensures it is present.
   *
   * @param model the name of the LLM model to pull
   * @return true if the LLM is successfully pulled or already present, false otherwise
   * @throws IOException if an I/O error occurs
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  public static boolean pullSingleLLM(String model) throws IOException, InterruptedException {
    Config config = getConfig();
    LOGGER.info("Checking if LLM is present: {}", model);
    if (!isLLMPresent(config, model)) {
      LOGGER.info("Pulling LLM: {}", model);
      if (!executeCMD(config.adjustCmd("pull " + model), CMDType.RUN, true, config)) {
        LOGGER.error("Error pulling LLM: {}", model);
        return false;
      }
    } else {
      LOGGER.info("LLM {} is already present. Skipping...", model);
    }
    return true;
  }

  /**
   * Checks if a specific LLM (Language Learning Model) is present.
   *
   * @param config the configuration for Ollama
   * @param model the name of the LLM model to check
   * @return true if the LLM is present, false otherwise
   * @throws IOException if an I/O error occurs
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  private static boolean isLLMPresent(Config config, String model) throws IOException, InterruptedException {
    return executeCMD(config.adjustCmd("show " + model), CMDType.RUN, false, config);
  }

  /**
   * Checks if Ollama is installed by executing a command.
   *
   * @param config the configuration for Ollama
   * @return true if Ollama is installed, false otherwise
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  private static boolean IsOllamaInstalled(Config config) throws InterruptedException {
    try {
      return executeCMD(config.adjustCmd(""), CMDType.RUN, false, config);
    } catch (IOException e) {
      if (e.getMessage().contains("error=2")) {
        return false;
      }
      throw new RuntimeException(e);
    }
  }

  /**
   * Installs Ollama by downloading and extracting the necessary files.
   *
   * @param config the configuration for Ollama
   * @return true if the installation was successful, false otherwise
   * @throws IOException if an I/O error occurs
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  private static boolean installOllama(Config config) throws IOException, InterruptedException {
    var llmConfig = Contexts.globalContext().get(LLMConfig.class).orElse(LLMConfig.defaultConfig());
    var url = "https://github.com/ollama/ollama/releases/download/" + llmConfig.version() + "/" + config.file;

    var tempPath = createTempPath();
    var filePath = tempPath.resolve(config.file);
    return downloadOllama(url, filePath) && extractOllama(filePath, config);
  }

  /**
   * Downloads the Ollama file from the specified URL to the given destination path.
   *
   * @param url the URL to download the Ollama file from
   * @param destination the path to save the downloaded file
   * @return true if the download was successful, false otherwise
   * @throws IOException if an I/O error occurs
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  private static boolean downloadOllama(String url, Path destination) throws IOException, InterruptedException {
    LOGGER.info("Downloading Ollama from: {}", url);
    try (var client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build()) {
      var request = HttpRequest.newBuilder()
              .uri(URI.create(url))
              .build();

      var response = client.send(request, HttpResponse.BodyHandlers.ofFile(destination));
      if (response.statusCode() != 200) {
        LOGGER.error("Error downloading Ollama: {}", response.statusCode());
        return false;
      }
      LOGGER.info("Ollama downloaded: {}", destination.getFileName());
      return true;
    }
  }

  /**
   * Extracts the downloaded Ollama file to the specified destination.
   *
   * @param file the path to the downloaded file
   * @param config the configuration for Ollama
   * @return true if the extraction was successful, false otherwise
   * @throws IOException if an I/O error occurs
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  private static boolean extractOllama(Path file, Config config) throws IOException, InterruptedException {
    LOGGER.info("Extracting file : {}", file.getFileName());
    var dest = config.ollamaPath();
    Files.createDirectories(dest);
    var cmd = config.extractCmd()
            .replace("%src%", file.toString())
            .replace("%dest%", dest.toString());

    var success = executeCMD(cmd, CMDType.RUN, false, config);

    if (success) {
      LOGGER.info("File successfully extracted to {}.", dest);
    } else {
      LOGGER.error("Error extracting file: {}", file.getFileName());
    }
    return success;
  }

  /**
   * Creates a temporary directory for the Ollama installation process.
   * The directory will be automatically deleted on exit.
   *
   * @return the path to the temporary directory
   * @throws IOException if an I/O error occurs
   */
  private static Path createTempPath() throws IOException {
    var tempDir = Files.createTempDirectory("ollama-install");
    tempDir.toFile().deleteOnExit();
    return tempDir;
  }

  /**
   * Enum representing the types of command execution.
   */
  private enum CMDType {
    RUN, RUN_NO_WAIT
  }

  /**
   * Executes a command using a ProcessBuilder.
   *
   * @param cmd the command to execute
   * @param type the type of command execution
   * @param inheritIO whether to inherit IO streams
   * @param config the configuration for Ollama
   * @return true if the command executed successfully, false otherwise
   * @throws IOException if an I/O error occurs
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  private static boolean executeCMD(String cmd, CMDType type, boolean inheritIO, Config config) throws IOException, InterruptedException {
    var processBuilder = getProcessBuilder(cmd, inheritIO, config);
    var process = processBuilder.start();
    if (type == CMDType.RUN_NO_WAIT) {
      return true;
    }
    if (process.waitFor() == 0) {
      LOGGER.info("Command executed successfully: {}", cmd);
      return true;
    }
    LOGGER.error("Error executing command: {}", cmd);
    return false;
  }

  /**
   * Creates a ProcessBuilder for executing a command.
   *
   * @param cmd the command to execute
   * @param inheritIO whether to inherit IO streams
   * @param config the configuration for Ollama
   * @return the configured ProcessBuilder
   */
  private static ProcessBuilder getProcessBuilder(String cmd, boolean inheritIO, Config config) {
    var processBuilder = new ProcessBuilder(cmd.split(" "));
    if (inheritIO) {
      processBuilder.inheritIO();
    }
    var env = processBuilder.environment();
    var url = Contexts.globalContext().get(LLMConfig.class).orElse(LLMConfig.defaultConfig()).urlAndPort();

    env.put("OLLAMA_MODELS", config.ollamaPath.resolve("models").toString());
    env.put("OLLAMA_HOST", url);
    return processBuilder;
  }


  public static void stopOllama() throws IOException, InterruptedException {
    var config = getConfig();
    executeCMD(config.killCmd(), CMDType.RUN, false, config);
  }
}
