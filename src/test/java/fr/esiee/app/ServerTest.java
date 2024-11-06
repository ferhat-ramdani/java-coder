package fr.esiee.app;

import fr.esiee.app.db.DbManager;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import java.io.IOException;

@io.helidon.webserver.testing.junit5.ServerTest
public class ServerTest {

  private final Http1Client client;

  public ServerTest(Http1Client client) {
    this.client = client;
  }

  @SetUpRoute
  static void routing(HttpRouting.Builder builder) throws IOException {
    DbManager.initialize();
    Main.routing(builder);
  }
}
