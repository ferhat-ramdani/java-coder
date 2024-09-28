package fr.esiee.app.llmcheck;

import fr.esiee.app.Main;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class OllamaCheck {

  private static final Logger LOGGER = LoggerFactory.getLogger(OllamaCheck.class);

  private static final String OLLAMA_URL = "https://ollama.com";
  private static final String LINUX_INSTALL_CMD = "curl -fsSL https://ollama.com/install.sh | sh";
  private static final String MAC_INSTALLER_CMD = "brew install --cask ollama";
  private static final String WINDOWS_INSTALLER_URL = "https://ollama.com/download/OllamaSetup.exe";
  private static final String MAC_LINUX_OLLAMA_VERSION_CMD = "ollama --version";
  private static final String WINDOWS_OLLAMA_CMD = "where ollama";

  public static boolean installOllama() throws IOException, InterruptedException {
    if (SystemUtils.IS_OS_LINUX) {
      return checkAndInstall(MAC_LINUX_OLLAMA_VERSION_CMD, "Linux", OllamaCheck::installOnLinux);
    } else if (SystemUtils.IS_OS_WINDOWS) {
      return checkAndInstall(WINDOWS_OLLAMA_CMD, "Windows", OllamaCheck::installOnWindows);
    } else if (SystemUtils.IS_OS_MAC) {
      return checkAndInstall(MAC_LINUX_OLLAMA_VERSION_CMD, "macOS", OllamaCheck::installOnMac);
    } else {
      printErrorInstall("Unsupported operating system.");
    }
    return false;
  }

  private static boolean checkAndInstall(String checkCmd, String osName, InstallFunction installFunction)
          throws IOException, InterruptedException {
    if (isOllamaInstalled(checkCmd)) {
      LOGGER.info("Ollama is already installed on " + osName + ".");
      return true;
    } else {
      return installFunction.install();
    }
  }

  private static boolean isOllamaInstalled(String command) {
    try {
      Process process = new ProcessBuilder(command.split(" ")).start();
      int exitCode = process.waitFor();
      return exitCode == 0;
    } catch (IOException | InterruptedException e) {
      return false;
    }
  }

  private static String getOSName() {
    return SystemUtils.OS_NAME.toLowerCase();
  }

  private static void printErrorInstall(String message) {
    LOGGER.error("Error installing on " + getOSName() + ".\n" + message);
    LOGGER.error("You need to install Ollama manually from " + OLLAMA_URL);
  }

  private static boolean installOnLinux() throws InterruptedException, IOException {
    LOGGER.info("Installing Ollama on Linux...");
    LOGGER.info("Enter sudo password");
    Process process = new ProcessBuilder("sudo", "-S", "sh", "-c", LINUX_INSTALL_CMD).inheritIO().start();
    process.waitFor();
    if (process.exitValue() == 0) {
      LOGGER.info("Ollama successfully installed on Linux.");
      return true;
    } else {
      printErrorInstall("Error executing the installation command.");
    }
    return false;
  }

  private static boolean installOnWindows() throws IOException, InterruptedException {
    LOGGER.info("Downloading the installation file for Windows...");
    String destination = "OllamaSetup.exe";
    downloadFile(destination);

    LOGGER.info("Running the installation file for Windows...");
    Process process = new ProcessBuilder("cmd", "/c", destination).inheritIO().start();
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
    Process process = new ProcessBuilder(MAC_INSTALLER_CMD.split(" ")).inheritIO().start();
    process.waitFor();
    if (process.exitValue() == 0) {
      LOGGER.info("Ollama successfully installed on macOS.");
      return true;
    } else {
      printErrorInstall("Failed to install Ollama via Homebrew.");
    }

    return false;
  }

  private static void downloadFile(String destination) throws IOException {
    var uri = URI.create(OllamaCheck.WINDOWS_INSTALLER_URL);
    try (var in = uri.toURL().openStream()) {
      Files.copy(in, Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);
    }
    LOGGER.info("File downloaded: " + destination);
  }

  @FunctionalInterface
  private interface InstallFunction {
    boolean install() throws IOException, InterruptedException;
  }
}
