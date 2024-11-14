package fr.esiee.app.services;

import fr.esiee.app.db.DbManager;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.helidon.http.HttpMediaTypes.JSON_PREDICATE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RoutingTest
public class ApiRoutingServiceTest {

  private final Http1Client client;

  public ApiRoutingServiceTest(Http1Client client) {
    this.client = client;
  }

  @SetUpRoute
  static void routing(HttpRouting.Builder builder) throws IOException {
    DbManager.initialize();
    builder.register("/", new ApiRoutingService());
  }

  @Test
  public void testLlmEndpoint() {
    try (var response = client.get("/llm").request()) {
      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertTrue(JSON_PREDICATE.test(response.headers().contentType().orElseThrow())));
    }
  }

  @Test
  public void testChatEndpoint() {
    try (var response = client.get("/chat").request()) {
      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertTrue(JSON_PREDICATE.test(response.headers().contentType().orElseThrow())));
    }
  }

  @Test
  public void testPromptEndpoint() {
    try (var response = client.get("/prompt").request()) {
      assertAll(() -> assertEquals(Status.OK_200, response.status()),
              () -> assertTrue(JSON_PREDICATE.test(response.headers().contentType().orElseThrow())));
    }
  }

  @Test
  public void testInvalidEndpoint() {
    try (var response = client.get("/inside").request()) {
      assertAll(() -> assertEquals(Status.BAD_REQUEST_400, response.status()),
              () -> assertTrue(JSON_PREDICATE.test(response.headers().contentType().orElseThrow())));
    }
  }
}
