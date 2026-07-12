package fr.esiee.app.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

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
  class CreateTempPathTests {
    @Test
    void testCreateTempPath() throws IOException {
      var tempPath = createTempPath();
      assertAll(
              () -> assertNotNull(tempPath, "Temp path should not be null"),
              () -> assertTrue(Files.exists(tempPath), "Temp path should exist"),
              () -> assertTrue(Files.isDirectory(tempPath), "Temp path should be a directory"),
              () -> assertTrue(Files.isReadable(tempPath), "Temp path should be readable"),
              () -> assertTrue(Files.isWritable(tempPath), "Temp path should be writable")
      );
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
  class CompileTests {
    @Test
    void testCompile_SuccessfulCompilationLeavesClassFile() throws IOException {
      var code = "public class Greeter { public static void main(String[] args) { System.out.println(\"Hi\"); } }";
      var result = compile(code);
      try {
        assertAll(
                () -> assertTrue(result.success(), "Compilation should succeed"),
                () -> assertEquals("Greeter", result.className()),
                () -> assertTrue(Files.exists(result.classDir().resolve("Greeter.class")), "Compiled class file should exist")
        );
      } finally {
        cleanup(result.classDir());
      }
    }

    @Test
    void testCompile_WithErrors() throws IOException {
      var code = "public class Broken { public static void main(String[] args) { System.out.println(\"Hello\" } }";
      var result = compile(code);
      try {
        assertFalse(result.success(), "Compilation should fail with errors");
      } finally {
        cleanup(result.classDir());
      }
    }

    @Test
    void testCompile_NoClassNameThrows() {
      assertThrows(IOException.class, () -> compile("not valid java at all"));
    }
  }

  @Nested
  class CleanupTests {
    @Test
    void testCleanup_RemovesDirectoryAndContents() throws IOException {
      var code = "public class ToClean { public static void main(String[] args) {} }";
      var result = compile(code);
      assertTrue(Files.exists(result.classDir()));
      cleanup(result.classDir());
      assertFalse(Files.exists(result.classDir()));
    }

    @Test
    void testCleanup_NonExistentDirectoryIsNoop() {
      assertDoesNotThrow(() -> cleanup(Paths.get("this-directory-does-not-exist-at-all")));
    }

    @Test
    void testCleanup_NullIsNoop() {
      assertDoesNotThrow(() -> cleanup(null));
    }
  }

  @Nested
  class HasMainMethodTests {
    @Test
    void testHasMainMethod_ValidMain() throws IOException {
      var code = "public class WithMain { public static void main(String[] args) {} }";
      var result = compile(code);
      try {
        assertTrue(hasMainMethod(result.classDir(), result.className()));
      } finally {
        cleanup(result.classDir());
      }
    }

    @Test
    void testHasMainMethod_NoMain() throws IOException {
      var code = "public class WithoutMain { public void run() {} }";
      var result = compile(code);
      try {
        assertFalse(hasMainMethod(result.classDir(), result.className()));
      } finally {
        cleanup(result.classDir());
      }
    }

    @Test
    void testHasMainMethod_NonPublicMainDoesNotCount() throws IOException {
      var code = "public class PrivateMain { static void main(String[] args) {} }";
      var result = compile(code);
      try {
        assertFalse(hasMainMethod(result.classDir(), result.className()));
      } finally {
        cleanup(result.classDir());
      }
    }

    @Test
    void testHasMainMethod_ClassNotFound() throws IOException {
      var tempDir = Files.createTempDirectory("test");
      tempDir.toFile().deleteOnExit();
      assertFalse(hasMainMethod(tempDir, "NoSuchClass"));
    }

    @Test
    void testHasMainMethod_DoesNotRunStaticInitializer() throws IOException {
      var code = "public class WithSideEffect { "
              + "static { if (true) throw new RuntimeException(\"static init ran\"); } "
              + "public static void main(String[] args) {} }";
      var result = compile(code);
      try {
        assertDoesNotThrow(() -> hasMainMethod(result.classDir(), result.className()));
        assertTrue(hasMainMethod(result.classDir(), result.className()));
      } finally {
        cleanup(result.classDir());
      }
    }
  }
}
