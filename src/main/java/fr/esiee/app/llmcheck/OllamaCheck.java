package fr.esiee.app.llmcheck;

import fr.esiee.app.config.LLMProviderConfig;
import fr.esiee.app.services.DbService;
import io.helidon.common.context.Contexts;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class OllamaCheck {

  private static final Logger LOGGER = LoggerFactory.getLogger(OllamaCheck.class);

  private static final String LOCAL_PATH = SystemUtils.USER_HOME + "/.chatgptfordev";
  private static final String LINUX_OLLAMA_PATH = LOCAL_PATH + "/bin";

  private static final String OLLAMA_VERSION = "v0.3.13";

  private static final String WINDOWS_FILE = "ollama-windows-" + SystemUtils.OS_ARCH + ".zip";
  private static final String LINUX_FILE = "ollama-linux-" + SystemUtils.OS_ARCH + ".tgz";

  private static final String WINDOWS_URL = "https://github.com/ollama/ollama/releases/download/" + OLLAMA_VERSION + "/" + WINDOWS_FILE;
  private static final String LINUX_URL = "https://github.com/ollama/ollama/releases/download/" + OLLAMA_VERSION + "/" + LINUX_FILE;

  private static final String MAC_CMD = "brew install ollama";

  private static final String WINDOWS_CMD_PREFIX = "cmd /c ";

  private static final String CMD_PREFIX = "ollama";
  private static final String SHOW_CMD = CMD_PREFIX + " show";
  private static final String SERVE_CMD = CMD_PREFIX + " serve";
  private static final String PULL_CMD = CMD_PREFIX + " pull";

  private static final String UNIX_CHECK_CMD = "which " + CMD_PREFIX;
  private static final String WINDOWS_CHECK_CMD = "where " + CMD_PREFIX;


  public static void init() throws IOException, InterruptedException {
    var cmd = SystemUtils.IS_OS_WINDOWS ? WINDOWS_CHECK_CMD : UNIX_CHECK_CMD;

    if (!executeCMD(cmd, CMDType.OTHER, false)) {
      LOGGER.info("Installing Ollama...");
      install();
      LOGGER.info("Ollama installed successfully.");
    } else {
      LOGGER.info("Ollama is already installed.");
    }
    start();
    pullLLMS();
  }

  private static void install() throws IOException, InterruptedException {
    String url, file;

    if (SystemUtils.IS_OS_LINUX) {
      url = LINUX_URL;
      file = LINUX_FILE;
    } else if (SystemUtils.IS_OS_WINDOWS) {
      url = WINDOWS_URL;
      file = WINDOWS_FILE;
    } else if (SystemUtils.IS_OS_MAC) {
      executeCMD(MAC_CMD, CMDType.OTHER, true);
      return;
    } else {
      throw new UnsupportedOperationException("Unsupported OS.");
    }

    var tempPath = createTempPath();
    downloadFile(url, tempPath.resolve(file));
    extractFile(tempPath.resolve(file), Paths.get(LOCAL_PATH));
  }

  private static void downloadFile(String url, Path destination) throws IOException {
    LOGGER.info("Downloading Ollama from: {}", url);
    try (var in = URI.create(url).toURL().openStream()) {
      Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
    }
    LOGGER.info("File downloaded: {}", destination.getFileName());
  }

  private static void extractFile(Path file, Path dest) throws IOException, InterruptedException {
    LOGGER.info("Extracting file: {}", file.getFileName());
    Files.createDirectories(dest);
    var cmd = "tar -xzf " + file + " -C " + dest;
    if (SystemUtils.IS_OS_WINDOWS) {
      cmd = "powershell -command \"Expand-Archive -Path '" + file + "' -DestinationPath '" + dest + "' -Force\"";
    }

    var exitCode = executeCMD(cmd, CMDType.OTHER, false);
    if (exitCode) {
      LOGGER.info("File successfully extracted to {}.", dest);
    } else {
      LOGGER.error("Error extracting file.");
    }
  }

  private static void start() throws IOException, InterruptedException {
    LOGGER.info("Starting Ollama.");
    executeCMD(SERVE_CMD, CMDType.RUN_OLLAMA, false);
    LOGGER.info("Ollama started successfully.");
  }


  private static Path createTempPath() throws IOException {
    var tempDir = Files.createTempDirectory("ollama-install");
    tempDir.toFile().deleteOnExit();
    return tempDir;
  }

  private static boolean executeCMD(String cmd, CMDType type, boolean inheritIO) throws IOException, InterruptedException {
    if(type == CMDType.RUN_OLLAMA) {
      if (SystemUtils.IS_OS_WINDOWS) {
        cmd = WINDOWS_CMD_PREFIX + cmd;
      } else if (SystemUtils.IS_OS_LINUX) {
        cmd = LINUX_OLLAMA_PATH + "/" + cmd;
      }
    }

    var processBuilder = new ProcessBuilder(cmd.split(" "));

    if (inheritIO) {
      processBuilder.inheritIO();
    }

    var env = processBuilder.environment();

    if (SystemUtils.IS_OS_WINDOWS) {
      env.put("Path", env.get("Path") + ";" + LOCAL_PATH + "/");
    } else {
      env.put("PATH", env.get("PATH") + ":" + LINUX_OLLAMA_PATH + "/");
    }

    var url = Contexts.globalContext().get(LLMProviderConfig.class).orElse(LLMProviderConfig.defaultConfig()).UrlAndPort();

    env.put("OLLAMA_MODELS", LOCAL_PATH + "/models");
    env.put("OLLAMA_HOST", url);

    var process = processBuilder.start();

    if (type == CMDType.RUN_OLLAMA) {
      return true;
    }
    if (process.waitFor() == 0) {
      LOGGER.info("Command executed successfully: {}", cmd);
      return true;
    }
    LOGGER.error("Error executing command: {}", cmd);
    return false;
  }

  private static boolean isLLMPresent(String model) throws IOException, InterruptedException {
    return executeCMD(SHOW_CMD + " " + model, CMDType.RUN_OLLAMA, false);
  }

  private static void pullLLMS() throws IOException, InterruptedException {
    var llmList = Contexts.globalContext().get(DbService.class).orElse(DbService.getInstance()).listLLMs();
    LOGGER.info("Waiting for LLMs to be pulled: to pull {} LLMs.", llmList.size());
    for (int i = 0; i < llmList.size(); i++) {
      var llm = llmList.get(i);
      LOGGER.info("Checking if LLM is present: {} - {}/{}", llm.model(), i + 1, llmList.size());
      if (!isLLMPresent(llm.model())) {
        executeCMD(PULL_CMD + " " + llm.model(), CMDType.RUN_OLLAMA, true);
      } else {
        LOGGER.info("LLM {} is already present. Skipping...", llm.model());
      }
    }
    LOGGER.info("LLMs checked and pulled successfully.");
  }

  private enum CMDType {
    RUN_OLLAMA, OTHER
  }
}
