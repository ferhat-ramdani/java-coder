package fr.esiee.app.sandbox;

/**
 * A single event streamed to the front-end about a live sandbox execution session.
 *
 * @param type the kind of event
 * @param data OUTPUT: a chunk of the program's stdout/stderr. EXITED: the process exit code.
 *             ERROR: a human-readable message explaining why the session ended abnormally.
 */
public record SandboxEvent(Type type, String data) {

  public enum Type { OUTPUT, EXITED, ERROR }
}
