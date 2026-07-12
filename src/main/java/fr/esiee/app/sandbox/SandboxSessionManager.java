package fr.esiee.app.sandbox;

import fr.esiee.app.utils.CompileAndExecUtils;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of live interactive sandbox sessions, keyed by session id.
 */
public final class SandboxSessionManager {

  private static final SandboxSessionManager INSTANCE = new SandboxSessionManager();

  private final Map<String, SandboxSession> sessions = new ConcurrentHashMap<>();

  private SandboxSessionManager() {
  }

  public static SandboxSessionManager instance() {
    return INSTANCE;
  }

  /**
   * Compiles the given source code and starts a new interactive sandbox session running it.
   *
   * @param sourceCode the Java source code to compile and run
   * @return the newly started session
   * @throws IOException          if compilation fails, the class has no main method, or the
   *                               sandbox could not be started
   * @throws InterruptedException if interrupted while starting the sandbox
   */
  public SandboxSession start(String sourceCode) throws IOException, InterruptedException {
    var compilation = CompileAndExecUtils.compile(sourceCode);
    if (!compilation.success()) {
      CompileAndExecUtils.cleanup(compilation.classDir());
      throw new IOException("Code does not compile:\n" + compilation.errors());
    }
    if (!CompileAndExecUtils.hasMainMethod(compilation.classDir(), compilation.className())) {
      CompileAndExecUtils.cleanup(compilation.classDir());
      throw new IOException("Generated class has no `public static void main(String[] args)` method.");
    }

    SandboxProcess process;
    try {
      process = SandboxExecutor.startInteractive(compilation.classDir(), compilation.className());
    } catch (IOException | InterruptedException e) {
      CompileAndExecUtils.cleanup(compilation.classDir());
      throw e;
    }

    var id = UUID.randomUUID().toString();
    var session = new SandboxSession(id, compilation.classDir(), process);
    sessions.put(id, session);
    session.activate(() -> sessions.remove(id));
    return session;
  }

  public SandboxSession get(String id) {
    return sessions.get(id);
  }

  public void remove(String id) {
    var session = sessions.remove(id);
    if (session != null) {
      session.terminate();
    }
  }
}
