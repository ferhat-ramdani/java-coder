package fr.esiee.app;

import fr.esiee.app.db.DbManager;
import io.helidon.common.context.Contexts;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import static org.mockito.Mockito.mock;

@io.helidon.webserver.testing.junit5.ServerTest
public class ServerTest {

  private final Http1Client client;

  public ServerTest(Http1Client client) {
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
}
