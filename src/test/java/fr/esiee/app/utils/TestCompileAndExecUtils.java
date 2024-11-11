package fr.esiee.app.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static fr.esiee.app.utils.CompileAndExecUtils.extractClassName;
import static fr.esiee.app.utils.CompileAndExecUtils.extractCode;
import static org.junit.jupiter.api.Assertions.*;

public class TestCompileAndExecUtils {

  @Nested
  class ExtractClassNameTests {

    @Test
    public void testExtractClassName_AllCases() {
      var validInput = "public class TestClass {}";
      var longClassInput = "public class LongClass { public void method() {} }";
      var invalidKeywordsInput = "public clss InvalidClass {}";
      var noClassNameInput = "public class {}";
      var twoClassNamesInput = "public class FirstClass {} public class SecondClass {}";
      var noPublicKeywordInput = "class NoPublicClass {}";
      var noClassKeywordInput = "public NoClassKeyword {}";
      var randomTextInput = "hello world";
      assertAll(
        () -> assertEquals(Optional.of("TestClass"), extractClassName(validInput)),
        () -> assertEquals(Optional.of("LongClass"), extractClassName(longClassInput)),
        () -> assertEquals(Optional.empty(), extractClassName(invalidKeywordsInput)),
        () -> assertEquals(Optional.empty(), extractClassName(noClassNameInput)),
        () -> assertEquals(Optional.of("FirstClass"), extractClassName(twoClassNamesInput)),
        () -> assertEquals(Optional.empty(), extractClassName(noPublicKeywordInput)),
        () -> assertEquals(Optional.empty(), extractClassName(noClassKeywordInput)),
        () -> assertEquals(Optional.empty(), extractClassName(randomTextInput))
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
}
