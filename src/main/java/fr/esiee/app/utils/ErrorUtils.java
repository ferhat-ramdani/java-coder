package fr.esiee.app.utils;

import io.helidon.http.Status;
import io.helidon.webserver.http.ServerResponse;

import java.time.LocalDateTime;

public class ErrorUtils {
  private record Error(String message, String timestamp, int statusCode, String statusMessage) { }

  public static void send(ServerResponse res, Status status, String message) {
    var err = new Error(message, LocalDateTime.now().toString(), status.code(), status.reasonPhrase());
    res.status(status).send(err);
  }

}
