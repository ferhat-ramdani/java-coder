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

  private static final String OLLAMA_URL = "https://ollama.com";
  private static final String MAC_INSTALLER_CMD = "brew install --cask ollama";
  private static final String WINDOWS_INSTALLER_URL = "https://ollama.com/download/OllamaSetup.exe";
  private static final String LINUX_INSTALLER_URL =
          "https://github.com/ollama/ollama/releases/download/v0.3.12/ollama-linux-amd64.tgz";
  private static final String OLLAMA_VERSION_CMD = "ollama --version";
  private static final String LOCAL_PATH = SystemUtils.USER_HOME + "/.chatgptfordev";
  private static final String LOCAL_OLLAMA_PATH = LOCAL_PATH + "/bin/ollama";


  private static boolean LOCAL_OLLAMA = false;


  private OllamaCheck() {
    throw new IllegalStateException("Utility class");
  }

  public static void init() {
    try {
      if (!installOllama()) {
        return;
      }
      if (LOCAL_OLLAMA) {
        runLocalLinux();
      }
      pullLLM(LOCAL_OLLAMA ? LOCAL_OLLAMA_PATH : "ollama");
    } catch (IOException | InterruptedException e) {
      LOGGER.error("Error initializing Ollama.");
    }
  }

  private static boolean installOllama() throws IOException, InterruptedException {
    LOGGER.info("Checking if Ollama is installed...");
    if (SystemUtils.IS_OS_LINUX) {
      if (isOllamaInstalled(OLLAMA_VERSION_CMD)) {
        LOGGER.info("Ollama is already installed globally on Linux.");
        return true;
      } else if (checkInstalledLocallyLinux()) {
        LOGGER.info("Ollama is installed locally.");
        LOCAL_OLLAMA = true;
        return true;
      } else {
        LOGGER.info("Ollama is not installed. Installing locally...");
        LOCAL_OLLAMA = installOnLinux();
        return LOCAL_OLLAMA;
      }
    } else if (SystemUtils.IS_OS_WINDOWS) {
      return checkAndInstall(OLLAMA_VERSION_CMD, "Windows", OllamaCheck::installOnWindows);
    } else if (SystemUtils.IS_OS_MAC) {
      return checkAndInstall(OLLAMA_VERSION_CMD, "macOS", OllamaCheck::installOnMac);
    } else {
      printErrorInstall("Unsupported operating system.");
    }
    return false;
  }

  private static boolean checkAndInstall(String checkCmd, String osName, InstallFunction installFunction)
          throws IOException, InterruptedException {
    if (isOllamaInstalled(checkCmd)) {
      LOGGER.info("Ollama is already installed on {}.", osName);
      return true;
    } else {
      LOGGER.info("Ollama is not installed on {}. Installing...", osName);
      return installFunction.install();
    }
  }

  private static boolean isOllamaInstalled(String command) {
    try {
      var process = new ProcessBuilder(command.split(" ")).start();
      return process.waitFor() == 0;
    } catch (IOException | InterruptedException e) {
      return false;
    }
  }

  private static void printErrorInstall(String message) {
    LOGGER.error("Error installing on {}", SystemUtils.OS_NAME.toLowerCase());
    LOGGER.error(message);
    LOGGER.error("You need to install Ollama manually from " + OLLAMA_URL);
  }

  private static boolean installOnWindows() throws IOException, InterruptedException {
    LOGGER.info("Downloading the installation file for Windows...");

    var destination = createTempPath().resolve("OllamaSetup.exe");
    downloadFile(WINDOWS_INSTALLER_URL, destination);
    LOGGER.info("Running the installation file for Windows...");
    var process = new ProcessBuilder("cmd", "/c", destination.toString()).inheritIO().start();
    process.waitFor();
    if (process.exitValue() == 0) {
      LOGGER.info("Ollama successfully installed on Windows.");
      return true;
    } else {
      printErrorInstall("Error running the installation file.");
    }
    return false;
  }

  private static boolean installOnMac() throws InterruptedException, IOException {
    LOGGER.info("Installing Ollama on macOS...");
    var process = new ProcessBuilder(MAC_INSTALLER_CMD.split(" ")).inheritIO().start();
    process.waitFor();
    if (process.exitValue() == 0) {
      LOGGER.info("Ollama successfully installed on macOS.");
      return true;
    } else {
      printErrorInstall("Failed to install Ollama via Homebrew.");
    }
    return false;
  }

  private static void downloadFile(String fileUrl, Path destination) throws IOException {
    LOGGER.info("Downloading file from: {}", fileUrl);
    try (var in = URI.create(fileUrl).toURL().openStream()) {
      Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
    }
    LOGGER.info("File downloaded: {}", destination);
  }

  private static boolean installOnLinux() {
    try {
      var destination = createTempPath().resolve("ollama-linux-amd64.tgz");
      downloadFile(LINUX_INSTALLER_URL, destination);
      extractTarGz(destination);
      return true;
    } catch (IOException | InterruptedException e) {
      printErrorInstall("Error installing locally Ollama on Linux.");
      return false;
    }
  }

  private static void runLocalLinux() {
    try {
      LOGGER.info("Running Ollama locally...");
      var process = new ProcessBuilder(LOCAL_OLLAMA_PATH, "serve").start();
      LOGGER.info("Ollama is now running.");
    } catch (IOException e) {
      printErrorInstall("Error running locally Ollama on Linux.");
    }
  }

  private static boolean checkInstalledLocallyLinux() {
    try {
      var process = new ProcessBuilder(LOCAL_OLLAMA_PATH, "--version").start();
      return process.waitFor() == 0;
    } catch (IOException | InterruptedException e) {
      return false;
    }
  }

  private static void extractTarGz(Path tarFilePath) throws IOException, InterruptedException {
    LOGGER.info("Extracting archive...");
    Files.createDirectories(Paths.get(OllamaCheck.LOCAL_PATH));
    var process = new ProcessBuilder("tar", "-xzf", tarFilePath.toString(), "-C", OllamaCheck.LOCAL_PATH).inheritIO().start();
    if (process.waitFor() != 0) {
      throw new IOException("Error extracting TAR.GZ archive");
    }
    LOGGER.info("Archive extracted successfully.");
  }

  private static void pullLLM(String cmd) {
    try {
      var llmList = DbService.getInstance().listLLMs();
      LOGGER.info("Waiting for LLMs to be pulled: to pull {} LLMs.", llmList.size());
      for (int i = 0; i < llmList.size(); i++) {
        var llm = llmList.get(i);
        LOGGER.info("Checking if LLM is present: {} - {}/{}", llm.model(), i + 1, llmList.size());
        if (!isLLMPresent(cmd, llm.model())) {
          pullLLM(cmd, llm.model());
        } else {
          LOGGER.info("LLM {} is already present. Skipping...", llm.model());
        }
      }
      LOGGER.info("LLMs checked and pulled successfully.");
    } catch (IOException | InterruptedException e) {
      LOGGER.error("Error pulling LLM.");
    }
  }

  private static boolean isLLMPresent(String cmd, String model) throws IOException, InterruptedException {
    var process = new ProcessBuilder(cmd, "show", model).start();
    return process.waitFor() == 0;
  }

  private static void pullLLM(String cmd, String model) throws IOException, InterruptedException {
    LOGGER.info("Pulling LLM: {}", model);
    var process = new ProcessBuilder(cmd, "pull", model).start();
    process.waitFor();
  }

  private static Path createTempPath() throws IOException {
    var tempDir = Files.createTempDirectory("ollama-install");
    tempDir.toFile().deleteOnExit();
    return tempDir;
  }

  @FunctionalInterface
  private interface InstallFunction {
    boolean install() throws IOException, InterruptedException;
  }
}
