package fr.esiee.app.llms;

import fr.esiee.app.config.LLMConfig;
import fr.esiee.app.db.DbManager;
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
import java.util.Map;
import java.util.NoSuchElementException;

public class OllamaSetupManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(OllamaSetupManager.class);

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

//  private static final String UNIX_CHECK_CMD = "which " + CMD_PREFIX;
//  private static final String WINDOWS_CHECK_CMD = "where " + CMD_PREFIX;


  public static void setupOllamaAndLLMs() throws IOException, InterruptedException {
//    var cmd = SystemUtils.IS_OS_WINDOWS ? WINDOWS_CHECK_CMD : UNIX_CHECK_CMD;
//    if (!executeCMD(cmd, CMDType.OTHER, false)) {
    if(!IsOllamaInstalled()){
      LOGGER.info("Installing Ollama...");
      installOllama();
      LOGGER.info("Ollama installed successfully.");
    } else {
      LOGGER.info("Ollama is already installed.");
    }
    startOllama();
    pullLLMS();
  }

  private static boolean IsOllamaInstalled() {
    var strPath = SystemUtils.IS_OS_WINDOWS ? LOCAL_PATH + "/" + CMD_PREFIX + ".exe" : LINUX_OLLAMA_PATH+"/" + CMD_PREFIX;
    var path = Paths.get(strPath);
    return Files.exists(path) && Files.isRegularFile(path) && Files.isExecutable(path);
  }

  private static void installOllama() throws IOException, InterruptedException {
    if (SystemUtils.IS_OS_MAC) {
      executeCMD(MAC_CMD, CMDType.OTHER, true);
      return;
    }

    var osConfig = switch (getOS()) {
      case LINUX -> Map.of("url", LINUX_URL, "file", LINUX_FILE);
      case WINDOWS -> Map.of("url", WINDOWS_URL, "file", WINDOWS_FILE);
      default -> throw new UnsupportedOperationException("Unsupported OS.");
    };

    var tempPath = createTempPath();
    var filePath = tempPath.resolve(osConfig.get("file"));
    downloadOllama(osConfig.get("url"), filePath);
    extractOllama(filePath, Paths.get(LOCAL_PATH));
  }

  private static void downloadOllama(String url, Path destination) throws IOException {
    LOGGER.info("Downloading Ollama from: {}", url);
    try (var in = URI.create(url).toURL().openStream()) {
      Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
    }
    LOGGER.info("Ollama downloaded: {}", destination.getFileName());
  }

  private static void extractOllama(Path file, Path dest) throws IOException, InterruptedException {
    LOGGER.info("Extracting file : {}", file.getFileName());
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

  private static void startOllama() throws IOException, InterruptedException {
    LOGGER.info("Starting Ollama.");
    executeCMD(SERVE_CMD, CMDType.RUN_OLLAM_NO_WAIT, false);
    LOGGER.info("Ollama started successfully.");
  }

  private static Path createTempPath() throws IOException {
    var tempDir = Files.createTempDirectory("ollama-install");
    tempDir.toFile().deleteOnExit();
    return tempDir;
  }

  private static String adjustCommandForOS(String cmd, CMDType type) {
    if (type == CMDType.OTHER) return cmd;

    return switch (getOS()) {
      case WINDOWS -> WINDOWS_CMD_PREFIX + cmd;
      case LINUX -> LINUX_OLLAMA_PATH + "/" + cmd;
      default -> cmd;
    };
  }

  private static boolean executeCMD(String cmd, CMDType type, boolean inheritIO) throws IOException, InterruptedException {
    cmd = adjustCommandForOS(cmd, type);
    var processBuilder = new ProcessBuilder(cmd.split(" "));
    if (inheritIO) {
      processBuilder.inheritIO();
    }
    var env = processBuilder.environment();
    var url = Contexts.globalContext().get(LLMConfig.class).orElse(LLMConfig.defaultConfig()).urlAndPort();
    env.put("Path", env.get("Path") + ";" + (SystemUtils.IS_OS_WINDOWS ? LOCAL_PATH : LINUX_OLLAMA_PATH) + "/");
    env.put("OLLAMA_MODELS", LOCAL_PATH + "/models");
    env.put("OLLAMA_HOST", url);
    var process = processBuilder.start();
    if (type == CMDType.RUN_OLLAM_NO_WAIT) {
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
    var llmList = Contexts.globalContext()
            .get(DbManager.class)
            .orElseThrow(() -> new NoSuchElementException("DbManager not found."))
            .listLLMs();
    LOGGER.info("Waiting for {} LLMs to be pulled.", llmList.size());
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

  private enum OS { LINUX, WINDOWS, MAC, UNKNOWN }

  private static OS getOS() {
    if (SystemUtils.IS_OS_LINUX) return OS.LINUX;
    if (SystemUtils.IS_OS_WINDOWS) return OS.WINDOWS;
    if (SystemUtils.IS_OS_MAC) return OS.MAC;
    return OS.UNKNOWN;
  }

  private enum CMDType {
    RUN_OLLAMA, RUN_OLLAM_NO_WAIT, OTHER
  }
}
