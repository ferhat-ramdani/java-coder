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

class CompileService {
  static List<String> compileJavaFile(String javaFilePath) {
    var compiler = ToolProvider.getSystemJavaCompiler();
    var diagnostics = new DiagnosticCollector<JavaFileObject>();
    var fileManager = compiler.getStandardFileManager(diagnostics, null, null);

    var compilationUnits = fileManager.getJavaFileObjects(javaFilePath);
    var task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);

    var success = task.call();
    var errors = new ArrayList<String>();

    if (success) {
      System.out.println("Compilation successful.");
    } else {
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

  public static void addExecutePermission(String filePath) throws IOException {
    var path = Paths.get(filePath);
    var permissions = Files.getPosixFilePermissions(path);
    permissions.add(PosixFilePermission.OWNER_EXECUTE);
    Files.setPosixFilePermissions(path, permissions);
  }

  static String executeClassFile(String classDirectory, String className) throws IOException, InterruptedException {
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

  static void deleteClassFile(String classFilePath) {
    var classFile = new File(classFilePath);

    if (classFile.exists()) {
      if (classFile.delete()) {
        System.out.println("Class file deleted: " + classFilePath);
      } else {
        System.out.println("Failed to delete class file: " + classFilePath);
      }
    } else {
      System.out.println("Class file not found: " + classFilePath);
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    var javaFilePath = "./src/main/resources/DummyClasses/DummyClass.java";
    var classDirectory = "./src/main/resources/DummyClasses";
    var className = "DummyClass";

    var compileErrors = compileJavaFile(javaFilePath);
    if (!compileErrors.isEmpty()) {
      compileErrors.forEach(System.out::println);
    } else {
      addExecutePermission(classDirectory + "/" + className + ".class");
      var output = executeClassFile(classDirectory, className);
      System.out.println("execution output : " + output);
      deleteClassFile(classDirectory + "/" + className + ".class");
    }
  }
}
