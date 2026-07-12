package fr.esiee.app.sandbox;

import fr.esiee.app.utils.CompileAndExecUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These tests exercise the real Docker sandbox, so they only run when a Docker daemon is
 * reachable (same pattern as the existing POSIX-only permission test): CI/dev machines without
 * Docker installed or running simply skip them instead of failing the build.
 */
class TestSandboxExecutor {

  private CompileAndExecUtils.CompilationResult compilation;

  @BeforeEach
  void checkDockerAvailable() {
    Assumptions.assumeTrue(DockerManager.isAvailable(), "Docker is not available, skipping sandbox tests.");
  }

  @AfterEach
  void cleanupCompilation() throws IOException {
    if (compilation != null) {
      CompileAndExecUtils.cleanup(compilation.classDir());
      compilation = null;
    }
  }

  private CompileAndExecUtils.CompilationResult compile(String code) throws IOException {
    compilation = CompileAndExecUtils.compile(code);
    assertTrue(compilation.success(), "Test fixture code should compile cleanly: " + compilation.errors());
    return compilation;
  }

  @Test
  void testRunSmokeTest_OkOnCleanExit() throws Exception {
    var result = compile("public class Hello { public static void main(String[] args) { System.out.println(\"hi\"); } }");
    var smoke = SandboxExecutor.runSmokeTest(result.classDir(), result.className());
    assertEquals(SandboxExecutor.Outcome.OK, smoke.outcome());
    assertTrue(smoke.output().contains("hi"));
  }

  @Test
  void testRunSmokeTest_RuntimeExceptionDetected() throws Exception {
    var result = compile("public class Boom { public static void main(String[] args) { throw new RuntimeException(\"kaboom\"); } }");
    var smoke = SandboxExecutor.runSmokeTest(result.classDir(), result.className());
    assertEquals(SandboxExecutor.Outcome.RUNTIME_EXCEPTION, smoke.outcome());
    assertTrue(smoke.output().contains("kaboom"));
  }

  @Test
  void testRunSmokeTest_BlockedOnInputIsNotAFailure() throws Exception {
    var result = compile("import java.util.Scanner; public class ReadsInput { "
            + "public static void main(String[] args) { new Scanner(System.in).nextLine(); } }");
    var smoke = SandboxExecutor.runSmokeTest(result.classDir(), result.className());
    assertEquals(SandboxExecutor.Outcome.BLOCKED_ON_INPUT, smoke.outcome());
  }

  @Test
  void testStartInteractive_EchoesProvidedInput() throws Exception {
    var result = compile("import java.util.Scanner; public class Echo { "
            + "public static void main(String[] args) { "
            + "Scanner sc = new Scanner(System.in); System.out.println(\"echo: \" + sc.nextLine()); } }");
    try (var process = SandboxExecutor.startInteractive(result.classDir(), result.className())) {
      process.writeLine("hello sandbox");
      assertDoesNotThrow(() -> process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS));
      var output = new String(process.stdout().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
      assertTrue(output.contains("echo: hello sandbox"), "Expected echoed input in output, got: " + output);
    }
  }
}
