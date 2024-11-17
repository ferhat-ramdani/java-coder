package fr.esiee.app.utils;

import io.helidon.http.Status;
import io.helidon.webserver.http.ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

public class ErrorUtils {

  // We don't have injection, so we need to use this declaration.
  private static final Logger LOGGER = LoggerFactory.getLogger(ErrorUtils.class);

  private record Error(String message, String timestamp, int statusCode, String statusMessage) { }

  public static void send(ServerResponse res, Status status, String message) {
    LOGGER.error(message);
    var err = new Error(message, LocalDateTime.now().toString(), status.code(), status.reasonPhrase());
    res.status(status).send(err);
  }

}
