package fr.esiee.app.utils;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static fr.esiee.app.utils.CompileAndExecUtils.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestCompileAndExecUtils {

  @Nested
  class ExtractClassNameTests {

    @Test
    public void testExtractClassName_AllCases() {
      assertAll(
              () -> assertEquals(Optional.of("TestClass"), extractClassName("public class TestClass {}")),
              () -> assertEquals(Optional.of("LongClass"), extractClassName("public class LongClass { public void method() {} }")),
              () -> assertEquals(Optional.empty(), extractClassName("public clss InvalidClass {}")),
              () -> assertEquals(Optional.empty(), extractClassName("public class {}")),
              () -> assertEquals(Optional.of("FirstClass"), extractClassName("public class FirstClass {} public class SecondClass {}")),
              () -> assertEquals(Optional.empty(), extractClassName("class NoPublicClass {}")),
              () -> assertEquals(Optional.empty(), extractClassName("public NoClassKeyword {}")),
              () -> assertEquals(Optional.empty(), extractClassName("hello world"))
      );
    }

    @Test
    public void testExtractClassName_EmptyAndNullInput() {
      assertAll(
        () -> assertEquals(Optional.empty(), extractClassName("")),
        () -> assertEquals(Optional.empty(), extractClassName(" ")),
        () -> assertThrows(NullPointerException.class, () -> extractClassName(null))
      );
    }
  }

  @Nested
  class ExtractCodeTests {

    @Test
    void testExtractCodeWithJavaBlock() {
      String text = "Here is some code:\n```java\npublic void method() {}\n```";
      String result = extractCode(text);
      assertEquals("public void method() {}", result);
    }

    @Test
    void testExtractCodeWithNonJavaBlock() {
      String text = "Here is some code:\n```\npublic void method() {}\n```";
      String result = extractCode(text);
      assertEquals("public void method() {}", result);
    }

    @Test
    void testExtractCodeWithoutCodeBlock() {
      String text = "Here is some text without code block";
      String result = extractCode(text);
      assertEquals(text, result);
    }

    @Test
    void testExtractCodeWithSingleDelimiter() {
      String text = "Here is some code with a single delimiter:\n```java\npublic void method() {}";
      String result = extractCode(text);
      assertEquals("public void method() {}", result);
    }

    @Test
    void testExtractCodeEmptyCodeBlock() {
      String text = "Empty code block:\n```java\n```";
      String result = extractCode(text);
      assertEquals("", result);
    }

    @Test
    void testExtractCodeNestedTextBeforeAndAfter() {
      String text = "Intro text\n```java\ncode inside\n```\nmore text";
      String result = extractCode(text);
      assertEquals("code inside", result);
    }
  }

  @Nested
  class DeleteFileTests {
    @Test
    void testCreateTempPath() throws IOException {
      var tempPath = createTempPath();
      assertAll(
              () -> assertNotNull(tempPath, "Temp path should not be null"),
              () -> assertTrue(Files.exists(tempPath), "Temp path should exist"),
              () -> assertTrue(Files.isDirectory(tempPath), "Temp path should be a directory"),
              () -> assertTrue(Files.isReadable(tempPath), "Temp path should be readable"),
              () -> assertTrue(Files.isWritable(tempPath), "Temp path should be writable"),
              () -> assertTrue(Files.isExecutable(tempPath), "Temp path should be executable")
      );
    }
  }

  @Nested
  class AddExecutePermission {
    @Test
    void testAddExecutePermission_AddsExecutePermission() throws IOException {
      var tempFile = Files.createTempFile("test-file", ".txt");
      tempFile.toFile().deleteOnExit();
      Assumptions.assumeTrue(Files.getFileStore(tempFile).supportsFileAttributeView("posix"));
      addExecutePermission(tempFile);
      var permissions = Files.getPosixFilePermissions(tempFile);
      assertTrue(permissions.contains(PosixFilePermission.OWNER_EXECUTE), "File should have execute permission for the owner");
    }

    @Test
    void testAddExecutePermission_ThrowsNullPointerExceptionForNullPath() {
      assertThrows(NullPointerException.class, () -> addExecutePermission(null), "Expected NullPointerException for null path");
    }
  }

  @Nested
  class CompileJavaFileTests {
    @Test
    void testCompileJavaFile_SuccessfulCompilation() throws Exception {
      var tempDir = Files.createTempDirectory("test");
      tempDir.toFile().deleteOnExit();
      var javaFile = tempDir.resolve("TestClass.java");
      var code = "public class TestClass { public static void main(String[] args) { System.out.println(\"Hello, World!\"); } }";
      Files.writeString(javaFile, code);
      var result = compileJavaFile(javaFile);
      assertEquals("", result);
    }

    @Test
    void testCompileJavaFile_InvalidJavaFile() throws Exception {
      var tempDir = Files.createTempDirectory("test");
      tempDir.toFile().deleteOnExit();
      var javaFile = tempDir.resolve("InvalidClass.java");
      var code = "public class InvalidClass { public static void main(String[] args) { System.out.println(\"Hello World\"); }";
      Files.writeString(javaFile, code);
      var result = compileJavaFile(javaFile);
      assertFalse(result.isEmpty());
    }

    @Test
    void testCompileJavaFile_NonExistentFile() {
      var nonExistentPath = Paths.get("non_existent_file.java");
      var result = compileJavaFile(nonExistentPath);
      assertFalse(result.isEmpty());
    }

    @Test
    void testCompileJavaFile_EmptyFile() throws Exception {
      var tempDir = Files.createTempDirectory("test");
      tempDir.toFile().deleteOnExit();
      var javaFile = tempDir.resolve("EmptyClass.java");
      Files.createFile(javaFile);
      var result = compileJavaFile(javaFile);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  class ExecuteClassFileTests {
    @Test
    void testExecuteClassFile_ValidClassExecution() throws Exception {
      var tempDir = Files.createTempDirectory("test");
      tempDir.toFile().deleteOnExit();
      var javaFile = tempDir.resolve("TestClass.java");
      var code = "public class TestClass { public static void main(String[] args) { System.out.print(\"Hello, World!\"); } }";
      Files.writeString(javaFile, code);
      compileJavaFile(javaFile);
      var output = executeClassFile(tempDir, "TestClass");
      assertEquals("Hello, World!", output);
    }

    @Test
    void testExecuteClassFile_NonExistentClass() throws IOException, ExecutionException, InterruptedException {
      var tempDir = Files.createTempDirectory("test").toFile();
      tempDir.deleteOnExit();
      assertFalse(executeClassFile(tempDir.toPath(), "NonExistentClass").isBlank());
    }

    @Test
    void testExecuteClassFile_InterruptedExecution() throws IOException {
      var tempDir = Files.createTempDirectory("test").toFile();
      tempDir.deleteOnExit();
      assertThrows(InterruptedException.class, () -> {
        Thread.currentThread().interrupt();
        executeClassFile(tempDir.toPath(), "AnyClass");
      });
    }

    @Test
    void testExecuteClassFile_InvalidClassDirectory() throws IOException, ExecutionException, InterruptedException {
      var invalidDir = Paths.get("invalid_directory");
      assertFalse(executeClassFile(invalidDir, "TestClass").isBlank());
    }

    @Test
    void testExecuteClassFile_TimedOutExecution() throws Exception {
      var tempDir = Files.createTempDirectory("test");
      tempDir.toFile().deleteOnExit();
      var javaFile = tempDir.resolve("LongRunningClass.java");
      var code = "public class LongRunningClass { public static void main(String[] args) { try { Thread.sleep(20000); } catch (InterruptedException e) { System.out.print(\"Execution interrupted\"); } } }";
      Files.writeString(javaFile, code);
      compileJavaFile(javaFile);

      assertTimeoutPreemptively(Duration.ofSeconds(EXEC_TIMEOUT_SEC+1), () -> {
        var output = executeClassFile(tempDir, "LongRunningClass");
        assertTrue(output.contains("Execution timed out after " + EXEC_TIMEOUT_SEC + " seconds"), "Output should indicate timeout");
      });
    }
  }

  @Nested
  class ProcessTextTests {
    @Test
    void testProcessText_CompileOperation_SuccessfulCompilation() throws IOException, ExecutionException, InterruptedException {
      var code = "public class TestClass { public static void main(String[] args) {} }";
      var result = processText(code, Operation.COMPILE);
      assertTrue(result.isEmpty(), "Compilation should succeed with no errors");
    }

    @Test
    void testProcessText_CompileOperation_WithErrors() throws IOException, ExecutionException, InterruptedException {
      var code = "public class TestClass { public static void main(String[] args) { System.out.println(\"Hello\" } }"; // Missing parenthesis
      var result = processText(code, Operation.COMPILE);
      assertFalse(result.isEmpty(), "Compilation should fail with errors");
    }

    @Test
    void testProcessText_ExecuteOperation_ValidExecution() throws IOException, ExecutionException, InterruptedException {
      var code = "public class TestClass { public static void main(String[] args) { System.out.print(\"Hello, World!\"); } }";
      var result = processText(code, Operation.EXECUTE);
      assertEquals("Hello, World!", result);
    }

    @Test
    void testProcessText_ExecuteOperation_TimedOutExecution() {
      var code = "public class LongRunningClass { public static void main(String[] args) { try { Thread.sleep(20000); } catch (InterruptedException e) { System.out.print(\"Execution interrupted\"); } } }";

      assertTimeoutPreemptively(Duration.ofSeconds(EXEC_TIMEOUT_SEC + 2), () -> {
        var result = processText(code, Operation.EXECUTE);
        assertTrue(result.contains("Execution timed out after " + EXEC_TIMEOUT_SEC + " seconds"), "Output should indicate timeout");
      });
    }

    @Test
    void testProcessText_ExecuteOperation_WithExecutionError() throws IOException, ExecutionException, InterruptedException {
      var code = "public class TestClass { public static void main(String[] args) { throw new RuntimeException(\"Execution failed\"); } }";
      assertFalse(processText(code, Operation.EXECUTE).isBlank());
    }

  }
}
