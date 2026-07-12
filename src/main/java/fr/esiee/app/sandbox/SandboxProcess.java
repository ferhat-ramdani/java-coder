package fr.esiee.app.sandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A single running (or finished) container executing a compiled Java class inside the sandbox.
 * <p>
 * Every generated program runs as its own throwaway, network-less, resource-capped Docker
 * container instead of a bare host process: no network access, a read-only root filesystem
 * (only the mounted class directory and a small tmpfs are writable), dropped Linux capabilities,
 * and hard memory/CPU/process-count ceilings.
 */
public final class SandboxProcess implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(SandboxProcess.class);

  private final String containerName;
  private final Process process;

  private SandboxProcess(String containerName, Process process) {
    this.containerName = containerName;
    this.process = process;
  }

  /**
   * Starts a new sandboxed container running the given compiled class.
   *
   * @param classDir  the directory containing the compiled .class file(s), mounted read-only
   * @param className the fully qualified name of the class to run
   * @return a handle to the running container
   * @throws IOException if the container could not be started
   */
  static SandboxProcess start(Path classDir, String className) throws IOException {
    var containerName = "javacoder-sandbox-" + UUID.randomUUID();
    var command = buildRunCommand(containerName, classDir, className);
    var process = new ProcessBuilder(command)
            .redirectErrorStream(true)
            .start();
    return new SandboxProcess(containerName, process);
  }

  private static List<String> buildRunCommand(String containerName, Path classDir, String className) {
    return List.of(
            "docker", "run", "--rm", "-i",
            "--name", containerName,
            "--network", "none",
            "--memory", "128m",
            "--memory-swap", "128m",
            "--cpus", "0.5",
            "--pids-limit", "64",
            "--security-opt", "no-new-privileges",
            "--cap-drop", "ALL",
            "--read-only",
            "--tmpfs", "/tmp:rw,size=16m",
            "-v", classDir.toAbsolutePath() + ":/sandbox:ro",
            "-w", "/sandbox",
            DockerManager.SANDBOX_IMAGE,
            "java", "-Xmx96m", "-Xss8m", "-XX:+UseSerialGC", className
    );
  }

  /**
   * @return the container's stdout+stderr (merged), readable as they are produced
   */
  public InputStream stdout() {
    return process.getInputStream();
  }

  /**
   * Writes a line of text (as if typed by a user at a terminal) to the container's stdin.
   *
   * @param line the input line, without a trailing newline
   * @throws IOException if the input could not be written
   */
  public void writeLine(String line) throws IOException {
    var out = process.getOutputStream();
    out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
    out.flush();
  }

  public boolean isAlive() {
    return process.isAlive();
  }

  /**
   * Waits for the container to exit.
   *
   * @param timeout the maximum time to wait
   * @param unit    the time unit of the timeout
   * @return true if the container exited before the timeout elapsed
   * @throws InterruptedException if interrupted while waiting
   */
  public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
    return process.waitFor(timeout, unit);
  }

  public int exitValue() {
    return process.exitValue();
  }

  /**
   * Forcibly stops the container. Kills the local docker CLI process AND issues
   * {@code docker rm -f}: destroying the local process alone only kills the CLI client, the
   * remote container would otherwise keep running on the Docker daemon.
   */
  @Override
  public void close() {
    process.destroy();
    try {
      var cleanup = new ProcessBuilder("docker", "rm", "-f", containerName)
              .redirectErrorStream(true)
              .start();
      cleanup.getOutputStream().close();
      cleanup.getInputStream().transferTo(OutputStream.nullOutputStream());
      cleanup.waitFor(10, TimeUnit.SECONDS);
    } catch (IOException e) {
      LOGGER.debug("Could not force-remove sandbox container {}: {}", containerName, e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
