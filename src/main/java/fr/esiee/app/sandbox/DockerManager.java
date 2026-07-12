package fr.esiee.app.sandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * Checks Docker availability and makes sure the sandbox runtime image is present locally.
 * Generated code is never run on the host JVM: it always executes inside a throwaway,
 * network-less container built from {@link #SANDBOX_IMAGE}.
 */
public final class DockerManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(DockerManager.class);

  /**
   * Small JRE-only image, matched to {@link fr.esiee.app.utils.CompileAndExecUtils#SANDBOX_JAVA_RELEASE}
   * so that classes compiled on the host can run unmodified inside the container.
   */
  public static final String SANDBOX_IMAGE = "eclipse-temurin:21-jre-alpine";

  private static volatile boolean imagePulled = false;

  private DockerManager() {
  }

  /**
   * Checks whether the Docker daemon is reachable.
   *
   * @return true if a Docker daemon answered within a few seconds, false otherwise
   */
  public static boolean isAvailable() {
    try {
      var process = new ProcessBuilder("docker", "version", "--format", "{{.Server.Version}}")
              .redirectErrorStream(true)
              .start();
      process.getOutputStream().close();
      var completed = process.waitFor(5, TimeUnit.SECONDS);
      if (!completed) {
        process.destroyForcibly();
        return false;
      }
      return process.exitValue() == 0;
    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  /**
   * Pulls the sandbox runtime image if it isn't present locally yet. Safe and cheap to call
   * repeatedly: after the first successful check/pull it becomes a no-op.
   *
   * @throws IOException          if the image could not be pulled
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  public static synchronized void ensureImagePresent() throws IOException, InterruptedException {
    if (imagePulled) {
      return;
    }
    if (isImagePresent()) {
      imagePulled = true;
      return;
    }
    LOGGER.info("Pulling sandbox image {} (first run only)...", SANDBOX_IMAGE);
    var process = new ProcessBuilder("docker", "pull", SANDBOX_IMAGE)
            .redirectErrorStream(true)
            .start();
    process.getOutputStream().close();
    try (var reader = process.inputReader()) {
      reader.lines().forEach(line -> LOGGER.info("[docker pull] {}", line));
    }
    if (process.waitFor() != 0) {
      throw new IOException("Failed to pull sandbox image " + SANDBOX_IMAGE);
    }
    imagePulled = true;
    LOGGER.info("Sandbox image {} is ready.", SANDBOX_IMAGE);
  }

  private static boolean isImagePresent() throws IOException, InterruptedException {
    var process = new ProcessBuilder("docker", "image", "inspect", SANDBOX_IMAGE)
            .redirectErrorStream(true)
            .start();
    process.getOutputStream().close();
    process.getInputStream().transferTo(OutputStream.nullOutputStream());
    return process.waitFor() == 0;
  }
}
