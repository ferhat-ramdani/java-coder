package fr.esiee.app.services;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class CompileService {
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

  public static List<String> compileJavaClass(String javaContent, String className) throws IOException {
    Objects.requireNonNull(javaContent);
    Objects.requireNonNull(className);
    var targetDir = "target/compile";
    var javaFilePath = targetDir + "/" + className + ".java";
    var classFilePath = targetDir + "/" + className + ".class";

    try {
      writeToFile(javaContent, javaFilePath);
      return compileJavaFile(javaFilePath);
    } finally {
      deleteFile(javaFilePath);
      deleteFile(classFilePath);
    }
  }

  public static String compileAndExecuteJavaCode(String javaContent, String className) throws IOException, InterruptedException {
    Objects.requireNonNull(javaContent);
    Objects.requireNonNull(className);
    var targetDir = "target/compile";
    var javaFilePath = targetDir + "/" + className + ".java";
    var classFilePath = targetDir + "/" + className + ".class";

    try {
      writeToFile(javaContent, javaFilePath);
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

  public static void main(String[] args) throws IOException, InterruptedException {
    var content = "public class HelloWorld {\n" +
            "  public static void main(String[] args) {\n" +
            "    System.out.println(\"Hello, World!\");\n" +
            "  }\n" +
            "}\n";
    var output = compileAndExecuteJavaCode(content, "HelloWorld");
    System.out.println(output);
  }
}
