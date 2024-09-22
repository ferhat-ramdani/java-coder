package fr.esiee.app;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

class JavaClassCompiler {
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

  static void deleteClassFile(String javaFilePath) {
    var classFilePath = javaFilePath.replace(".java", ".class");
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
}
