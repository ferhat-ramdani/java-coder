package fr.esiee.app.utils;

import fr.esiee.app.exception.RestApiException;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompileAndExecUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(CompileAndExecUtils.class);
  static final int EXEC_TIMEOUT_SEC = 5;

  public enum Operation {
    COMPILE, EXECUTE
  }

  /**
   * Processes the given Java code by either compiling or executing it based on the specified operation.
   *
   * @param code the Java code to process
   * @param operation the operation to perform (either COMPILE or EXECUTE)
   * @return the result of the operation (compilation errors or execution output)
   * @throws IOException if an I/O error occurs
   * @throws InterruptedException if the execution is interrupted
   * @throws ExecutionException if an error occurs during execution
   */
  public static String processText(String code, Operation operation)
          throws IOException, InterruptedException, ExecutionException {
    Objects.requireNonNull(code);
    var classNameOpt = extractClassName(code);
    if (classNameOpt.isEmpty()) {
      throw new RestApiException("no class name could be extracted");
    }

    var className = classNameOpt.get();
    var targetDir = createTempPath();
    var javaFilePath = targetDir.resolve(className + ".java");
    var classFilePath = targetDir.resolve(className + ".class");

    try {
      return switch (operation) {
        case COMPILE -> compileJavaClass(code, className, javaFilePath);
        case EXECUTE -> compileAndExecuteJavaCode(code, className, targetDir, javaFilePath, classFilePath);
      };
    } finally {
      deleteFile(javaFilePath);
      deleteFile(classFilePath);
    }
  }

  /**
   * Compiles the given Java code and returns the compilation errors.
   *
   * @param javaCode the Java code to compile
   * @param className the name of the class to compile
   * @param javaFilePath the path to the Java file to be compiled
   * @return the compilation errors, or an empty string if the compilation was successful
   * @throws IOException if an I/O error occurs
   */
  private static String compileJavaClass(String javaCode, String className, Path javaFilePath) throws IOException {
    Objects.requireNonNull(javaCode);
    Objects.requireNonNull(className);
    Objects.requireNonNull(javaFilePath);

    Files.writeString(javaFilePath, javaCode);
    return compileJavaFile(javaFilePath);
  }

  /**
   * Compiles and executes the given Java code. If the execution takes longer than
   * <code>EXEC_TIMEOUT_SEC</code> seconds, the execution is stopped (and a message
   * is appended to the output).
   *
   * @param javaCode the Java code to compile and execute
   * @param className the name of the class to compile and execute
   * @param targetDir the directory to store the compiled files
   * @param javaFilePath the path to the Java file to be compiled
   * @param classFilePath the path to the compiled class file
   * @return the output of the executed class
   * @throws IOException if an I/O error occurs
   * @throws InterruptedException if the execution is interrupted
   * @throws ExecutionException if an error occurs during execution
   */
  private static String compileAndExecuteJavaCode(String javaCode, String className, Path targetDir, Path javaFilePath, Path classFilePath)
          throws IOException, InterruptedException, ExecutionException {
    Objects.requireNonNull(javaCode);
    Objects.requireNonNull(className);

    Files.writeString(javaFilePath, javaCode);
    var compileErrors = compileJavaFile(javaFilePath);
    if (!compileErrors.isEmpty()) {
      return compileErrors;
    }
    if(isPosixFamily()) {
      addExecutePermission(classFilePath);
    }
    return executeClassFile(targetDir, className);
  }

  /**
   * Checks if the current operating system is a Unix-like system.
   *
   * @return true if the operating system is a Unix-like system, false otherwise
   */
  private static boolean isPosixFamily() {
    return SystemUtils.IS_OS_UNIX || SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC;
  }

  /**
   * Executes the compiled Java class file. If the execution takes longer than
   * <code>EXEC_TIMEOUT_SEC</code> seconds, the execution is stopped (and a message
   * is appended to the output).
   *
   * @param classDirectory the directory containing the compiled class file
   * @param className the name of the class to execute
   * @return the output of the executed class
   * @throws IOException if an I/O error occurs
   * @throws InterruptedException if the execution is interrupted
   * @throws ExecutionException if an error occurs during execution
   */
  static String executeClassFile(Path classDirectory, String className)
          throws IOException, InterruptedException, ExecutionException {
    Objects.requireNonNull(classDirectory);
    Objects.requireNonNull(className);
    var process = new ProcessBuilder("java", "-cp", classDirectory.toString(), className)
            .redirectErrorStream(true)
            .start();
    try (var executor = Executors.newSingleThreadExecutor();
         var outputStream = new ByteArrayOutputStream()) {
      Future<?> outputFuture;
      try {
        outputFuture = executor.submit(() -> {
          try (var processOutput = process.getInputStream()) {
            processOutput.transferTo(outputStream);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
      } catch (UncheckedIOException e) {
        throw e.getCause();
      }
      var finished = process.waitFor(EXEC_TIMEOUT_SEC, TimeUnit.SECONDS);
      if (!finished) {
        LOGGER.warn("Execution of class {} timed out after {} seconds", className, EXEC_TIMEOUT_SEC);
        process.destroy();
      } else {
        outputFuture.get();
      }
      var output = outputStream.toString(StandardCharsets.UTF_8);
      if (!finished){
        output += "\nExecution timed out after " + EXEC_TIMEOUT_SEC + " seconds.\n";
      }
      return output;
    }
  }

  /**
   * Compiles the Java file at the specified path.
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
    var success = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits).call();
    var errors = new ArrayList<String>();

    if (!success) {
      for (var diagnostic : diagnostics.getDiagnostics()) {
        var error = String.format("Error on line %d in %s%n%s",
                diagnostic.getLineNumber(),
                diagnostic.getSource().getName(),
                diagnostic.getMessage(Locale.ROOT));
        errors.add(error);
      }
    }
    return String.join("\n", errors);
  }

  /**
   * Adds execute permission to the specified file path.
   *
   * @param path the path to the file to add execute permission to
   * @throws IOException if an I/O error occurs
   */
  static void addExecutePermission(Path path) throws IOException {
    Objects.requireNonNull(path);
    var permissions = Files.getPosixFilePermissions(path);
    permissions.add(PosixFilePermission.OWNER_EXECUTE);
    Files.setPosixFilePermissions(path, permissions);
  }

  /**
   * Deletes the specified file if it exists.
   *
   * @param filePath the path to the file to be deleted
   * @throws IOException if an I/O error occurs
   */
  private static void deleteFile(Path filePath) throws IOException {
    Objects.requireNonNull(filePath);
    if (Files.exists(filePath)) {
        Files.delete(filePath);
    }
  }

  /**
   * Creates a temporary directory path for storing compiled and executed files.
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
    if (firstDelimiter == -1) return text;

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
    String regex = "public\\s+(?:\\w+\\s+)*(?:class|record)\\s+(\\w+)(?:\\s*<.*?>)?\\s*(?:extends\\s+\\w+)?\\s*(?:implements\\s+[\\w,\\s]+)?";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(code);
    return Optional.ofNullable(matcher.find() ? matcher.group(1) : null);
  }
}
