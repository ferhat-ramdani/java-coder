package fr.esiee.app.utils;

import org.apache.commons.lang3.SystemUtils;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompileAndExecUtils {

  public enum Operation {
    COMPILE, EXECUTE
  }

  public static String processText(String code, Operation operation) throws IOException, InterruptedException {
    Objects.requireNonNull(code);
    var className = extractClassName(code);
    if (className.isEmpty()) {
      throw new IllegalStateException("no class name could be extracted");
    }
    return switch (operation) {
      case COMPILE -> compileJavaClass(code, className.get());
      case EXECUTE -> compileAndExecuteJavaCode(code, className.get());
    };
  }

  private static String compileJavaClass(String javaCode, String className) throws IOException {
    Objects.requireNonNull(javaCode);
    Objects.requireNonNull(className);
    var targetDir = createTempPath();
    var javaFilePath = targetDir.resolve(className + ".java");
    var classFilePath = targetDir.resolve(className + ".class");

    try {
      writeToFile(javaCode, javaFilePath);
      return compileJavaFile(javaFilePath);
    } finally {
      deleteFile(javaFilePath);
      deleteFile(classFilePath);
    }
  }

  private static String compileAndExecuteJavaCode(String javaCode, String className) throws IOException, InterruptedException {
    Objects.requireNonNull(javaCode);
    Objects.requireNonNull(className);
    var targetDir = createTempPath();
    var javaFilePath = targetDir.resolve(className + ".java");
    var classFilePath = targetDir.resolve(className + ".class");

    try {
      writeToFile(javaCode, javaFilePath);
      var compileErrors = compileJavaFile(javaFilePath);
      if (!compileErrors.isEmpty()) {
        return compileErrors;
      }
      if(isPosixFamily()) {
        addExecutePermission(classFilePath);
      }
      return executeClassFile(targetDir, className);
    } finally {
      deleteFile(classFilePath);
      deleteFile(javaFilePath);
    }
  }

  private static boolean isPosixFamily() {
    return SystemUtils.IS_OS_UNIX || SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC;
  }

  private static String executeClassFile(Path classDirectory, String className) throws IOException, InterruptedException {
    Objects.requireNonNull(classDirectory);
    Objects.requireNonNull(className);
    var output = "";
    try (var outputStream = new ByteArrayOutputStream();
         var printStream = new PrintStream(outputStream, true, StandardCharsets.UTF_8)) {
      var processBuilder = new ProcessBuilder("java", "-cp", classDirectory.toString(), className);
      var process = processBuilder.redirectErrorStream(true).start();
      try (var processOutput = process.getInputStream()) {
        processOutput.transferTo(printStream);
      }
      process.waitFor();
      output = outputStream.toString(StandardCharsets.UTF_8);
    }
    return output;
  }

  private static String compileJavaFile(Path javaFilePath) {
    Objects.requireNonNull(javaFilePath);
    var compiler = ToolProvider.getSystemJavaCompiler();
    var diagnostics = new DiagnosticCollector<JavaFileObject>();
    var fileManager = compiler.getStandardFileManager(diagnostics, null, null);
    var compilationUnits = fileManager.getJavaFileObjects(javaFilePath);
    var success = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits)
            .call();
    var errors = new ArrayList<String>();

    if (!success) {
      for (var diagnostic : diagnostics.getDiagnostics()) {
        var error = String.format("Error on line %d in %s%n%s",
                diagnostic.getLineNumber(),
                diagnostic.getSource().toUri(),
                diagnostic.getMessage(null));
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
