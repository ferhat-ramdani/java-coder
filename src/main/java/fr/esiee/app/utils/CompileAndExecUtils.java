package fr.esiee.app.utils;

import fr.esiee.app.exception.RestApiException;
import org.apache.commons.lang3.SystemUtils;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompileAndExecUtils {

  public static final int EXEC_TIMEOUT_SEC = 5;

  public enum Operation {
    COMPILE, EXECUTE
  }

  public static String processText(String code, Operation operation)
          throws IOException, InterruptedException, ExecutionException {
    Objects.requireNonNull(code);
    var classNameOpt = extractClassName(code);
    if (classNameOpt.isEmpty()) {
      return "No class name could be extracted";
//      throw new IllegalStateException("no class name could be extracted");
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

  private static String compileJavaClass(String javaCode, String className, Path javaFilePath) throws IOException {
    Objects.requireNonNull(javaCode);
    Objects.requireNonNull(className);
    Objects.requireNonNull(javaFilePath);

    writeToFile(javaCode, javaFilePath);
    return compileJavaFile(javaFilePath);
  }

  private static String compileAndExecuteJavaCode(String javaCode, String className, Path targetDir, Path javaFilePath, Path classFilePath)
          throws IOException, InterruptedException, ExecutionException {
    Objects.requireNonNull(javaCode);
    Objects.requireNonNull(className);

    writeToFile(javaCode, javaFilePath);
    var compileErrors = compileJavaFile(javaFilePath);
    if (!compileErrors.isEmpty()) {
      return compileErrors;
    }
    if(isPosixFamily()) {
      addExecutePermission(classFilePath);
    }
    return executeClassFile(targetDir, className);
  }

  private static boolean isPosixFamily() {
    return SystemUtils.IS_OS_UNIX || SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC;
  }

  private static String executeClassFile(Path classDirectory, String className)
          throws IOException, InterruptedException, ExecutionException {
    Objects.requireNonNull(classDirectory);
    Objects.requireNonNull(className);


    var process = new ProcessBuilder("java", "-cp", classDirectory.toString(), className)
            .redirectErrorStream(true)
            .start();

    try (var executor = Executors.newSingleThreadExecutor();
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      var outputFuture = executor.submit(() -> {
        try (var processOutput = process.getInputStream()) {
          processOutput.transferTo(outputStream);
        } catch (IOException e) {
          throw new RestApiException(e);
        }
      });

      var finished = process.waitFor(EXEC_TIMEOUT_SEC, TimeUnit.SECONDS);

      if (!finished) {
        process.destroy();
      }

      outputFuture.get();

      var output = outputStream.toString(StandardCharsets.UTF_8);
      if (!finished){
        return output + "\nExecution timed out after " + EXEC_TIMEOUT_SEC + " seconds.\n";
      }
      return output;
    }
  }

  private static String compileJavaFile(Path javaFilePath) {
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

  private static void addExecutePermission(Path path) throws IOException {
    Objects.requireNonNull(path);
    var permissions = Files.getPosixFilePermissions(path);
    permissions.add(PosixFilePermission.OWNER_EXECUTE);
    Files.setPosixFilePermissions(path, permissions);
  }

  private static void writeToFile(String content, Path filePath) throws IOException {
    Objects.requireNonNull(content);
    Files.createDirectories(filePath.getParent());
    Files.createFile(filePath);

    try (var writer = Files.newBufferedWriter(filePath)) {
      writer.write(content);
    }
  }

  private static void deleteFile(Path filePath) {
    Objects.requireNonNull(filePath);

    if (Files.exists(filePath)) {
      try {
        Files.delete(filePath);
      } catch (IOException e) {
        throw new RuntimeException("Cannot delete " + filePath, e);
      }
    }
  }

  private static Path createTempPath() throws IOException {
    var tempDir = Files.createTempDirectory("compile-exec-files");
    tempDir.toFile().deleteOnExit();
    return tempDir;
  }

  public static String extractCode(String text) {
    int firstDelimiter = text.indexOf("```java");
    int offset = 7;

    if (firstDelimiter == -1) {
      firstDelimiter = text.indexOf("```");
      offset = 3;
    }
    if (firstDelimiter == -1) return text;

    text = text.substring(firstDelimiter + offset);
    int secondDelimiter = text.indexOf("```");
    return secondDelimiter == -1 ? text : text.substring(0, secondDelimiter);
  }


  private static Optional<String> extractClassName(String code) {
    String regex = "public\\s+(?:\\w+\\s+)*(?:class|record)\\s+(\\w+)(?:\\s*<.*?>)?\\s*(?:extends\\s+\\w+)?\\s*(?:implements\\s+[\\w,\\s]+)?";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(code);
    return Optional.ofNullable(matcher.find() ? matcher.group(1) : null);
  }
}
