package fr.esiee.app.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CompileService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CompileService.class);

  public static List<String> processAndCompileText(String code) throws IOException {
    Objects.requireNonNull(code);
    var className = extractClassName(code);
    if (className.isEmpty()) {
      throw new IllegalStateException("no class name could be extracted");
    }
    return compileJavaClass(code, className.get());
  }

  public static String compileAndExecuteText(String text) throws IOException, InterruptedException {
    Objects.requireNonNull(text);
    String code = extractCode(text);
    var className = extractClassName(code);
    if (className.isEmpty()) {
      throw new IllegalStateException("no class name could be extracted");
    }
    return compileAndExecuteJavaCode(code, className.get());
  }

  private static List<String> compileJavaClass(String javaCode, String className) throws IOException {
    Objects.requireNonNull(javaCode);
    Objects.requireNonNull(className);
    var targetDir = createTempPath().toString();
    var javaFilePath = targetDir + "/" + className + ".java";
    var classFilePath = targetDir + "/" + className + ".class";

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
    var targetDir = createTempPath().toString();
    var javaFilePath = targetDir + "/" + className + ".java";
    var classFilePath = targetDir + "/" + className + ".class";

    try {
      writeToFile(javaCode, javaFilePath);
      var compileErrors = compileJavaFile(javaFilePath);
      if (!compileErrors.isEmpty()) {
        return String.join("\n", compileErrors);
      }
      addExecutePermission(classFilePath);
      return executeClassFile(targetDir, className);
    } finally {
      deleteFile(classFilePath);
      deleteFile(javaFilePath);
    }
  }

  private static List<String> compileJavaFile(String javaFilePath) {
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
    return errors;
  }

  private static void addExecutePermission(String filePath) throws IOException {
    Objects.requireNonNull(filePath);
    var path = Paths.get(filePath);
    var permissions = Files.getPosixFilePermissions(path);
    permissions.add(PosixFilePermission.OWNER_EXECUTE);
    Files.setPosixFilePermissions(path, permissions);
  }

  private static String executeClassFile(String classDirectory, String className) throws IOException, InterruptedException {
    Objects.requireNonNull(classDirectory);
    Objects.requireNonNull(className);
    var output = "";
    try (var outputStream = new ByteArrayOutputStream();
         var printStream = new PrintStream(outputStream, true, StandardCharsets.UTF_8)) {
      var processBuilder = new ProcessBuilder("java", "-cp", classDirectory, className);
      var process = processBuilder.redirectErrorStream(true).start();
      try (var processOutput = process.getInputStream()) {
        processOutput.transferTo(printStream);
      }
      process.waitFor();
      output = outputStream.toString(StandardCharsets.UTF_8);
    }
    return output;
  }

  private static void writeToFile(String content, String filePath) throws IOException {
    Objects.requireNonNull(content);
    var path = Paths.get(filePath);
    Files.createDirectories(path.getParent());
    Files.createFile(path);

    try (var writer = Files.newBufferedWriter(path)) {
      writer.write(content);
    }
  }

  private static void deleteFile(String filePath) {
    Objects.requireNonNull(filePath);
    var classFile = new File(filePath);

    if (classFile.exists()) {
      if (!classFile.delete()) {
        throw new RuntimeException("Cannot delete " + filePath);
      }
    }
  }

  private static Path createTempPath() throws IOException {
    var tempDir = Files.createTempDirectory("ollama-install");
    tempDir.toFile().deleteOnExit();
    return tempDir;
  }

  public static String extractCode(String text) {
    int firstDelimiter = text.indexOf("```java");
    if (firstDelimiter == -1) return text;
    text = text.substring(firstDelimiter + 7);
    int secondDelimiter = text.indexOf("```");
    return secondDelimiter == -1 ? text : text.substring(0, secondDelimiter);
  }

  private static Optional<String> extractClassName(String code) {
    String regex = "public\\s+(?:\\w+\\s+)*(?:class|record)\\s+(\\w+)(?:\\s*<.*?>)?\\s*(?:extends\\s+\\w+)?\\s*(?:implements\\s+[\\w,\\s]+)?";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(code);
    return Optional.ofNullable(matcher.find() ? matcher.group(1) : null);
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    var content = """
            Here's how you can generate a random number in Java:
            ```java
            import java.util.Random;
            public class RandomNumberGenerator {
              public static void main(String[] args) {
                Random random = new Random();
                int randomNumber = random.nextInt(100);  // Generates a random number between 0 and 99
                System.out.println("Generated random number: " + randomNumber);
              }
            }
            ```
            This code uses the Random class to generate numbers.
            """;
    var output = compileAndExecuteText(content);
    System.out.println(output);
  }
}
