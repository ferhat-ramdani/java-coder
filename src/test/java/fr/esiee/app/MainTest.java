package fr.esiee.app;


import fr.esiee.app.db.DbManager;
import io.helidon.common.context.Contexts;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@RoutingTest
public class MainTest {

  private final Http1Client client;

  public MainTest(Http1Client client) {
    this.client = client;
    var mockDbManager = mock(DbManager.class);
    Contexts.globalContext().register(mockDbManager);
  }

  @SetUpRoute
  static void routing(HttpRouting.Builder builder) {
    var mockDbManager = mock(DbManager.class);
    Contexts.globalContext().register(mockDbManager);
    Main.routing(builder);
  }

  @Test
  void testUiEndpoint() {
    try (var response = client.get("/index.html").request()) {
      assertEquals(response.status(), Status.OK_200);
      assertEquals(response.headers().contentType().orElseThrow().text(), "text/html");
    }
  }
}
