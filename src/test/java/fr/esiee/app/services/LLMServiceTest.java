package fr.esiee.app.services;

import fr.esiee.app.db.DbManager;
import fr.esiee.app.llms.LLMDTO;
import fr.esiee.tests.DbUtils;
import io.helidon.common.context.Contexts;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.helidon.http.HttpMediaTypes.JSON_PREDICATE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RoutingTest
class LLMServiceTest {

  private final DbManager dbManager;
  private final Http1Client client;


  @BeforeAll
  static void beforeAll() throws IOException {
    DbUtils.resetDb();
    DbUtils.initializeRealLLM();
  }

  public LLMServiceTest(Http1Client client) {
    this.client = client;
    dbManager = Contexts.globalContext().get(DbManager.class).orElseThrow();
  }

  @SetUpRoute
  static void routing(HttpRouting.Builder builder) throws IOException {
    DbManager.initialize();
    builder.register("/", new LLMService());
  }

  /**
   * The "installed" flag reflects whatever is actually present on disk for whoever runs the
   * tests, so equality checks below only compare the static fields (id, name, model) instead of
   * the full DTO.
   */
  private static void assertSameLLM(fr.esiee.app.db.entities.LLM expected, LLMDTO actual) {
    assertAll(() -> assertEquals(expected.id(), actual.id()),
            () -> assertEquals(expected.name(), actual.name()),
            () -> assertEquals(expected.model(), actual.model()));
  }

  @Test
  void testGetListOfLLM() {
    try (var response = client.get("/").request()) {
      var llmDtos = response.as(LLMDTO[].class);
      var llmsFromDb = dbManager.listLLMs();

      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertTrue(JSON_PREDICATE.test(response.headers().contentType().orElseThrow())),
              () -> assertEquals(llmDtos.length, llmsFromDb.size()), () -> assertEquals(3, llmDtos.length));
      for (int i = 0; i < llmsFromDb.size(); i++) {
        assertSameLLM(llmsFromDb.get(i), llmDtos[i]);
      }
    }
  }

  @Test
  void testGetFirstLLM() {
    try (var response = client.get("/first/llm").request()) {
      var llmDto = response.as(LLMDTO.class);
      var llmFromDb = dbManager.getFirstLLM();

      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertTrue(JSON_PREDICATE.test(response.headers().contentType().orElseThrow())));
      assertSameLLM(llmFromDb, llmDto);
    }
  }

  @Test
  void testGetLLMById() {
    try (var response = client.get("/1").request()) {
      var llmDto = response.as(LLMDTO.class);
      var llmFromDb = dbManager.getLLMById(1);

      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertTrue(JSON_PREDICATE.test(response.headers().contentType().orElseThrow())));
      assertSameLLM(llmFromDb, llmDto);
    }
  }
}
