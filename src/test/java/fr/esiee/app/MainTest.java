package fr.esiee.app;


import fr.esiee.app.db.DbManager;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.helidon.common.media.type.MediaTypes.TEXT_HTML;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@RoutingTest
public class MainTest {

  private final Http1Client client;

  public MainTest(Http1Client client) {
    this.client = client;
  }

  @SetUpRoute
  static void routing(HttpRouting.Builder builder) throws IOException {
    DbManager.initialize();
    Main.routing(builder);
  }

  @Test
  void testUiEndpoint() {
    try (var response = client.get("/index.html").request()) {
      assertAll(() -> assertEquals(response.status(), Status.OK_200),
              () -> assertEquals(TEXT_HTML, response.headers().contentType().orElseThrow()));
    }
  }
}
