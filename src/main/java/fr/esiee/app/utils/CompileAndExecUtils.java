package fr.esiee.app.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for compiling Java code and safely inspecting the result. Actually running
 * generated code never happens here (and never on the host JVM): that responsibility belongs to
 * the {@code fr.esiee.app.sandbox} package, which executes it in an isolated Docker container.
 */
public class CompileAndExecUtils {

  // We don't have injection, so we need to use this declaration.
  private static final Logger LOGGER = LoggerFactory.getLogger(CompileAndExecUtils.class);

  /**
   * Generated code is always compiled against this release, independent of the host JDK version,
   * so that the produced class files are guaranteed to run inside the sandbox's JRE image.
   */
  static final int SANDBOX_JAVA_RELEASE = 21;

  private CompileAndExecUtils() {
  }

  /**
   * The result of compiling a piece of source code: where the artifacts live, the class name
   * that was extracted from the source, and any compiler errors.
   *
   * @param classDir  the directory containing the source file and, if compilation succeeded, the
   *                   compiled .class file(s)
   * @param className the public class name extracted from the source
   * @param errors    the compiler diagnostics, or an empty string if compilation succeeded
   */
  public record CompilationResult(Path classDir, String className, String errors) {

    public boolean success() {
      return errors.isEmpty();
    }
  }

  /**
   * Writes the given source code to a fresh temporary directory and compiles it.
   * <p>
   * The caller is responsible for calling {@link #cleanup(Path)} on the returned
   * {@link CompilationResult#classDir()} once it is no longer needed (e.g. once the class has
   * been executed, or on a failed compile).
   *
   * @param code the Java source code to compile
   * @return the compilation result
   * @throws IOException if no class name could be extracted from the code, or an I/O error occurs
   */
  public static CompilationResult compile(String code) throws IOException {
    Objects.requireNonNull(code);
    var className = extractClassName(code)
            .orElseThrow(() -> new IOException("no class name could be extracted"));

    var targetDir = createTempPath();
    var javaFilePath = targetDir.resolve(className + ".java");
    Files.writeString(javaFilePath, code);
    var errors = compileJavaFile(javaFilePath);
    return new CompilationResult(targetDir, className, errors);
  }

  /**
   * Recursively deletes a compilation's temporary directory (source file, compiled classes,
   * including any inner/anonymous/lambda-generated .class files).
   *
   * @param dir the directory to delete, typically a {@link CompilationResult#classDir()}
   * @throws IOException if an I/O error occurs while deleting
   */
  public static void cleanup(Path dir) throws IOException {
    if (dir == null || !Files.exists(dir)) {
      return;
    }
    try (var paths = Files.walk(dir)) {
      paths.sorted(Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.delete(p);
        } catch (IOException e) {
          LOGGER.warn("Failed to delete {}: {}", p, e.getMessage());
        }
      });
    }
  }

  /**
   * Checks whether the compiled class exposes a standard {@code public static void main(String[])}
   * entry point. The class is loaded with {@code initialize=false}, so its static initializers
   * (and therefore any generated code) never run on the host: this is a pure bytecode/metadata
   * inspection, not an execution.
   *
   * @param classDir  the directory containing the compiled .class file
   * @param className the class to inspect
   * @return true if a public static main(String[]) method is present
   */
  public static boolean hasMainMethod(Path classDir, String className) {
    Objects.requireNonNull(classDir);
    Objects.requireNonNull(className);
    try (var loader = new URLClassLoader(new URL[]{classDir.toUri().toURL()}, ClassLoader.getSystemClassLoader())) {
      var cls = Class.forName(className, false, loader);
      var method = cls.getDeclaredMethod("main", String[].class);
      var modifiers = method.getModifiers();
      return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers);
    } catch (ClassNotFoundException | NoSuchMethodException | IOException | LinkageError e) {
      return false;
    }
  }

  /**
   * Compiles the Java file at the specified path against {@link #SANDBOX_JAVA_RELEASE}.
   *
   * @param javaFilePath the path to the Java file to compile
   * @return the compilation errors, or an empty string if the compilation was successful
   */
  static String compileJavaFile(Path javaFilePath) {
    Objects.requireNonNull(javaFilePath);
    var compiler = ToolProvider.getSystemJavaCompiler();
    var diagnostics = new DiagnosticCollector<JavaFileObject>();
    var fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8);
    var compilationUnits = fileManager.getJavaFileObjects(javaFilePath);
    var options = List.of("--release", String.valueOf(SANDBOX_JAVA_RELEASE));
    var success = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits).call();
    var errors = new ArrayList<String>();

    if (!success) {
      for (var diagnostic : diagnostics.getDiagnostics()) {
        var error = String.format("Error on line %d%n%s",
                diagnostic.getLineNumber(),
                diagnostic.getMessage(Locale.ROOT));
        errors.add(error);
      }
    }
    return String.join("\n", errors);
  }

  /**
   * Creates a temporary directory path for storing compiled files.
   *
   * @return the path to the temporary directory
   * @throws IOException if an I/O error occurs
   */
  static Path createTempPath() throws IOException {
    var tempDir = Files.createTempDirectory("compile-exec-files");
    tempDir.toFile().deleteOnExit();
    return tempDir;
  }

  /**
   * Extracts the code block from the given text.
   *
   * @param text the text containing the code block
   * @return the extracted code block, or the original text if no code block is found
   * @throws NullPointerException if the text is null
   */
  public static String extractCode(String text) {
    Objects.requireNonNull(text);
    int firstDelimiter = text.indexOf("```java");
    int offset = 7;

    if (firstDelimiter == -1) {
      firstDelimiter = text.indexOf("```");
      offset = 3;
    }
    if (firstDelimiter == -1) {
      return text;
    }

    int newLineOffsetStart = text.charAt(firstDelimiter + offset) == '\n' ? 1 : 0;
    text = text.substring(firstDelimiter + offset + newLineOffsetStart);
    int secondDelimiter = text.indexOf("```");
    int newLineOffsetEnd = (secondDelimiter > 0 && text.charAt(secondDelimiter - 1) == '\n') ? 1 : 0;
    return secondDelimiter == -1 ? text : text.substring(0, secondDelimiter - newLineOffsetEnd);
  }


  /**
   * Extracts the class name from the given Java code.
   *
   * @param code the Java code to extract the class name from
   * @return an Optional containing the class name if found, otherwise an empty Optional
   */
  static Optional<String> extractClassName(String code) {
    String regex =
            "public\\s+(?:\\w+\\s+)*(?:class|record)\\s+([\\p{L}\\w]+)(?:\\s*<.*?>)?\\s*(?:extends\\s+\\w+)?\\s*(?:implements\\s+[\\w,\\s]+)?";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(code);
    return Optional.ofNullable(matcher.find() ? matcher.group(1) : null);
  }
}
