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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static io.helidon.http.HttpMediaTypes.JSON_PREDICATE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RoutingTest
class LLMServiceTest {

  private final DbManager dbManager;
  private final Http1Client client;

  public LLMServiceTest(Http1Client client) throws IOException {
    DbUtils.resetDb();
    DbUtils.initializeRealLLM();
    this.client = client;
    dbManager = Contexts.globalContext().get(DbManager.class).orElseThrow();
  }

  @SetUpRoute
  static void routing(HttpRouting.Builder builder) throws IOException {
    DbManager.initialize();
    builder.register("/", new LLMService());
  }

  @Test
  void testGetListOfLLM() {
    try (var response = client.get("/").request()) {
      var llmDtos = response.as(LLMDTO[].class);
      var llmDtosFromDb = dbManager.listLLMs().stream().map(LLMDTO::copyOf).toList();

      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertTrue(JSON_PREDICATE.test(response.headers().contentType().orElseThrow())),
              () -> assertEquals(llmDtos.length, llmDtosFromDb.size()), () -> assertEquals(3, llmDtos.length),
              () -> assertEquals(List.of(llmDtos), llmDtosFromDb));
    }
  }

  @Test
  void testGetFirstLLM() {
    try (var response = client.get("/first/llm").request()) {
      var llmDto = response.as(LLMDTO.class);
      var llmDtoFromDb = LLMDTO.copyOf(dbManager.getFirstLLM());

      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertTrue(JSON_PREDICATE.test(response.headers().contentType().orElseThrow())),
              () -> assertEquals(llmDto, llmDtoFromDb));
    }
  }

  @Test
  void testGetLLMById() {
    try (var response = client.get("/1").request()) {
      var llmDto = response.as(LLMDTO.class);
      var llmDtoFromDb = LLMDTO.copyOf(dbManager.getLLMById(1));

      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertTrue(JSON_PREDICATE.test(response.headers().contentType().orElseThrow())),
              () -> assertEquals(llmDtoFromDb, llmDto));
    }
  }
}
