package fr.esiee.app.services;

import fr.esiee.app.db.DbManager;
import fr.esiee.app.db.entities.LLM;
import fr.esiee.app.llms.LLMDTO;
import io.helidon.common.context.Contexts;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RoutingTest
class LLMServiceTest {

  @Mock
  private static DbManager dbManager;
  private final Http1Client client;

  public LLMServiceTest(Http1Client client) {
    this.client = client;
  }

  @SetUpRoute
  static void routing(HttpRouting.Builder builder) throws IOException {
    dbManager = mock(DbManager.class);
    Contexts.globalContext().register(dbManager);
    var llm1 = new LLM(1, "LLM1", "1", "1", "1", .1, 1);
    var llm2 = new LLM(2, "LLM2", "2", "2", "2", .2, 2);
    var llm3 = new LLM(3, "LLM3", "3", "3", "3", .3, 3);
    when(dbManager.listLLMs()).thenReturn(List.of(llm1, llm2, llm3));
    when(dbManager.getFirstLLM()).thenReturn(llm1);
    when(dbManager.getLLMById(1)).thenReturn(llm1);

    builder.register("/", new LLMService());
  }

  @Test
  void testGetListOfLLM() {
    try (var response = client.get("/").request()) {
      var llmDtos = response.as(LLMDTO[].class);
      var llmDtosFromDb = dbManager.listLLMs().stream().map(LLMDTO::copyOf).toList();

      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertEquals(response.headers().contentType().orElseThrow().text(), "application/json"),
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
              () -> assertEquals(response.headers().contentType().orElseThrow().text(), "application/json"),
              () -> assertEquals(llmDto, llmDtoFromDb));
    }
  }

  @Test
  void testGetLLMById() {
    try (var response = client.get("/1").request()) {
      var llmDto = response.as(LLMDTO.class);
      var llmDtoFromDb = LLMDTO.copyOf(dbManager.getLLMById(1));

      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertEquals("application/json", response.headers().contentType().orElseThrow().text()),
              () -> assertEquals(llmDtoFromDb, llmDto));
    }
  }
}
