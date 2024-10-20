package fr.esiee.app.llmcheck;

import fr.esiee.app.services.DbService;
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

  public static void init() throws IOException, InterruptedException {
    var cmd = "which " + CMD_PREFIX;
    if (SystemUtils.IS_OS_WINDOWS) {
      cmd = WINDOWS_CMD_PREFIX + "where " + CMD_PREFIX;
    }
    var isInstalled = executeCMD(cmd, CMDType.RUN, false);

    if (!isInstalled) {
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
    String url = LINUX_URL;
    String file = LINUX_FILE;
    if (SystemUtils.IS_OS_WINDOWS) {
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
    var cmd = "tar -xvzf " + file + " -C " + dest;

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
    var cmd = SERVE_CMD + " &";
    var type = CMDType.RUN;
    if (SystemUtils.IS_OS_WINDOWS) {
      cmd = WINDOWS_CMD_PREFIX + SERVE_CMD;
      type = CMDType.RUN_NO_WAIT;
    }
    executeCMD(cmd, type, false);
    LOGGER.info("Ollama started successfully.");
  }


  private static Path createTempPath() throws IOException {
    var tempDir = Files.createTempDirectory("ollama-install");
    tempDir.toFile().deleteOnExit();
    return tempDir;
  }

  private static boolean executeCMD(String cmd, CMDType type, boolean inheritIO) throws IOException, InterruptedException {
    var processBuilder = new ProcessBuilder(cmd.split(" "));

    if (inheritIO) {
      processBuilder.inheritIO();
    }

    if (type == CMDType.RUN || type == CMDType.RUN_NO_WAIT) {
      var env = processBuilder.environment();
      env.put("Path", env.get("Path") + ";" + (SystemUtils.IS_OS_LINUX ? LINUX_OLLAMA_PATH : LOCAL_PATH) + "/");
      env.put("OLLAMA_MODELS", LOCAL_PATH + "/models");
    }

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

  private static boolean isLLMPresent(String model) throws IOException, InterruptedException {
    var cmd = SHOW_CMD + " " + model;
    if (SystemUtils.IS_OS_WINDOWS) {
      cmd = WINDOWS_CMD_PREFIX + cmd;
    }
    return executeCMD(cmd, CMDType.RUN, false);
  }

  private static void pullLLMS() throws IOException, InterruptedException {
    var llmList = DbService.getInstance().listLLMs();
    LOGGER.info("Waiting for LLMs to be pulled: to pull {} LLMs.", llmList.size());
    var cmd = PULL_CMD;
    if (SystemUtils.IS_OS_WINDOWS) {
      cmd = WINDOWS_CMD_PREFIX + cmd;
    }
    for (int i = 0; i < llmList.size(); i++) {
      var llm = llmList.get(i);
      LOGGER.info("Checking if LLM is present: {} - {}/{}", llm.model(), i + 1, llmList.size());
      if (!isLLMPresent(llm.model())) {
        executeCMD(cmd + " " + llm.model(), CMDType.RUN, true);
      } else {
        LOGGER.info("LLM {} is already present. Skipping...", llm.model());
      }
    }
    LOGGER.info("LLMs checked and pulled successfully.");
  }

  private enum CMDType {
    RUN, RUN_NO_WAIT, OTHER
  }
}
