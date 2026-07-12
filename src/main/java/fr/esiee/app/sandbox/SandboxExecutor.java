package fr.esiee.app.sandbox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Runs compiled code inside the Docker sandbox, either as a short unattended "smoke test" used
 * by the generation loop to catch broken code before showing it to the user, or as a live
 * interactive session wired up to a real terminal in the UI.
 */
public final class SandboxExecutor {

  private static final int SMOKE_TEST_TIMEOUT_SEC = 6;

  private SandboxExecutor() {
  }

  /**
   * The outcome of a smoke test run.
   */
  public enum Outcome {
    /** The program ran to completion without throwing. */
    OK,
    /** The program is still running/blocked at the timeout without having thrown anything yet.
     *  Its stdin was kept open (never closed/EOF'd), so this most likely means the program is
     *  legitimately waiting for user input rather than crashing — which is expected for
     *  interactive programs and is treated as a pass, not a failure. */
    BLOCKED_ON_INPUT,
    /** The program terminated with a non-zero exit code, i.e. it threw an uncaught exception. */
    RUNTIME_EXCEPTION,
    /** Docker itself is not available; the execution check could not be performed at all. */
    DOCKER_UNAVAILABLE
  }

  public record SmokeTestResult(Outcome outcome, String output) {
  }

  /**
   * Runs the compiled class in a sandboxed container for a short, bounded amount of time to
   * check that it doesn't immediately blow up. Stdin is left open (not fed, not closed) so that
   * a program legitimately waiting on user input can be told apart from one that is crashing.
   *
   * @param classDir  directory containing the compiled .class file(s)
   * @param className the class to run
   * @return the classification of what happened
   * @throws IOException          if the sandbox process could not be started
   * @throws InterruptedException if interrupted while waiting for the process
   */
  public static SmokeTestResult runSmokeTest(Path classDir, String className) throws IOException, InterruptedException {
    if (!DockerManager.isAvailable()) {
      return new SmokeTestResult(Outcome.DOCKER_UNAVAILABLE,
              "Docker is not available, so the generated code could not be executed to check for errors. "
                      + "Install and start Docker Desktop to enable this safety check.");
    }
    DockerManager.ensureImagePresent();

    try (var sandbox = SandboxProcess.start(classDir, className)) {
      var outputBuffer = new ByteArrayOutputStream();
      var pump = Thread.ofVirtual().start(() -> {
        try {
          sandbox.stdout().transferTo(outputBuffer);
        } catch (IOException ignored) {
          // the stream closes once the container exits or gets force-removed
        }
      });

      var exited = sandbox.waitFor(SMOKE_TEST_TIMEOUT_SEC, TimeUnit.SECONDS);
      if (!exited) {
        var partialOutput = outputBuffer.toString(StandardCharsets.UTF_8);
        return new SmokeTestResult(Outcome.BLOCKED_ON_INPUT, partialOutput);
      }

      pump.join(2000);
      var output = outputBuffer.toString(StandardCharsets.UTF_8);
      return sandbox.exitValue() == 0
              ? new SmokeTestResult(Outcome.OK, output)
              : new SmokeTestResult(Outcome.RUNTIME_EXCEPTION, output);
    }
  }

  /**
   * Starts a live, interactive sandboxed run of the compiled class with no fixed timeout: the
   * caller (a {@link SandboxSession}) is responsible for idle/absolute timeouts and cleanup.
   *
   * @param classDir  directory containing the compiled .class file(s)
   * @param className the class to run
   * @return a handle to the running container
   * @throws IOException           if the sandbox process could not be started
   * @throws InterruptedException  if interrupted while ensuring the sandbox image is present
   */
  public static SandboxProcess startInteractive(Path classDir, String className) throws IOException, InterruptedException {
    if (!DockerManager.isAvailable()) {
      throw new IOException("Docker is not available. Install and start Docker Desktop to run code.");
    }
    DockerManager.ensureImagePresent();
    return SandboxProcess.start(classDir, className);
  }
}
