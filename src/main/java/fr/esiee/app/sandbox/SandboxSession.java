package fr.esiee.app.sandbox;

import fr.esiee.app.utils.CompileAndExecUtils;
import io.helidon.http.sse.SseEvent;
import io.helidon.webserver.sse.SseSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A live, interactive sandboxed run of one generated class: an "as if it were a real terminal"
 * session that the front-end can attach to (to watch output live) and send keystrokes to.
 * <p>
 * Output is buffered so a client that attaches after the session started still gets full replay,
 * and the session survives a client disconnecting/reconnecting to the SSE stream. An idle and an
 * absolute timeout guarantee the underlying container is eventually torn down even if nobody is
 * watching.
 */
public final class SandboxSession {

  private static final Logger LOGGER = LoggerFactory.getLogger(SandboxSession.class);
  private static final long MAX_OUTPUT_BYTES = 200_000;
  private static final long IDLE_TIMEOUT_SEC = 180;
  private static final long ABSOLUTE_TIMEOUT_SEC = 600;
  private static final int REPLAY_BUFFER_LIMIT = 1000;

  private final String id;
  private final Path classDir;
  private final SandboxProcess process;
  private final Object lock = new Object();
  private final List<SandboxEvent> replayBuffer = new ArrayList<>();
  private final AtomicBoolean finished = new AtomicBoolean(false);
  private final CompletableFuture<Void> finishedFuture = new CompletableFuture<>();

  private SseSink attachedSink;
  private volatile Instant lastActivity = Instant.now();
  private volatile long outputBytes = 0;
  private Runnable onFinish;

  SandboxSession(String id, Path classDir, SandboxProcess process) {
    this.id = id;
    this.classDir = classDir;
    this.process = process;
  }

  public String id() {
    return id;
  }

  /**
   * Starts pumping the container's output and the idle/absolute timeout watchdog. Must be called
   * only after the session is registered wherever {@code onFinish} expects to remove it from, to
   * avoid a race where the process finishes before it's discoverable.
   *
   * @param onFinish called exactly once, when the session terminates for any reason
   */
  void activate(Runnable onFinish) {
    this.onFinish = onFinish;
    pumpOutput();
    watchdog();
  }

  private void pumpOutput() {
    Thread.ofVirtual().start(() -> {
      var buffer = new byte[4096];
      try {
        int read;
        while ((read = process.stdout().read(buffer)) != -1) {
          lastActivity = Instant.now();
          outputBytes += read;
          publish(new SandboxEvent(SandboxEvent.Type.OUTPUT, new String(buffer, 0, read, StandardCharsets.UTF_8)));
          if (outputBytes > MAX_OUTPUT_BYTES) {
            publish(new SandboxEvent(SandboxEvent.Type.ERROR, "Output limit exceeded, the process was terminated."));
            process.close();
            break;
          }
        }
      } catch (IOException ignored) {
        // stream closes once the container exits or gets force-removed
      } finally {
        finish();
      }
    });
  }

  private void watchdog() {
    Thread.ofVirtual().start(() -> {
      var start = Instant.now();
      while (!finished.get()) {
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
        var now = Instant.now();
        if (now.isAfter(lastActivity.plusSeconds(IDLE_TIMEOUT_SEC))) {
          publish(new SandboxEvent(SandboxEvent.Type.ERROR, "Session was idle for too long and was terminated."));
          process.close();
          return;
        }
        if (now.isAfter(start.plusSeconds(ABSOLUTE_TIMEOUT_SEC))) {
          publish(new SandboxEvent(SandboxEvent.Type.ERROR, "Session exceeded the maximum run time and was terminated."));
          process.close();
          return;
        }
      }
    });
  }

  private void finish() {
    if (!finished.compareAndSet(false, true)) {
      return;
    }
    int exitCode;
    try {
      process.waitFor(5, TimeUnit.SECONDS);
      exitCode = process.isAlive() ? -1 : process.exitValue();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      exitCode = -1;
    }
    publish(new SandboxEvent(SandboxEvent.Type.EXITED, String.valueOf(exitCode)));
    try {
      CompileAndExecUtils.cleanup(classDir);
    } catch (IOException e) {
      LOGGER.debug("Failed to clean up sandbox class directory {}: {}", classDir, e.getMessage());
    }
    if (onFinish != null) {
      onFinish.run();
    }
    finishedFuture.complete(null);
  }

  /**
   * Sends a line of input to the running program, as if a user had typed it at a terminal.
   *
   * @param input the input line, without a trailing newline
   * @throws IOException if the session already finished or the input could not be written
   */
  public void sendInput(String input) throws IOException {
    if (finished.get()) {
      throw new IOException("Session has already finished.");
    }
    lastActivity = Instant.now();
    process.writeLine(input);
  }

  /**
   * Attaches an SSE sink to this session: it immediately receives the full replay buffer, then
   * live events as they happen. Only one sink is attached at a time; attaching a new one replaces
   * the previous one.
   *
   * @param sink the sink to attach
   */
  public void attach(SseSink sink) {
    synchronized (lock) {
      attachedSink = sink;
      for (var event : replayBuffer) {
        emitSafely(sink, event);
      }
    }
  }

  /**
   * Detaches the given sink if it is the currently attached one; a no-op otherwise.
   *
   * @param sink the sink to detach
   */
  public void detach(SseSink sink) {
    synchronized (lock) {
      if (attachedSink == sink) {
        attachedSink = null;
      }
    }
  }

  /**
   * Blocks the calling thread until the session finishes (process exit, timeout, or forced
   * termination).
   */
  public void awaitFinished() {
    try {
      finishedFuture.get();
    } catch (Exception e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Forcibly stops the session, killing the underlying container.
   */
  public void terminate() {
    process.close();
  }

  private void publish(SandboxEvent event) {
    synchronized (lock) {
      replayBuffer.add(event);
      if (replayBuffer.size() > REPLAY_BUFFER_LIMIT) {
        replayBuffer.removeFirst();
      }
      if (attachedSink != null) {
        emitSafely(attachedSink, event);
      }
    }
  }

  private void emitSafely(SseSink sink, SandboxEvent event) {
    try {
      sink.emit(SseEvent.create(event));
    } catch (RuntimeException e) {
      LOGGER.debug("Failed to emit sandbox event, client likely disconnected: {}", e.getMessage());
    }
  }
}
