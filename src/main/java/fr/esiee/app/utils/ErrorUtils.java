package fr.esiee.app.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ErrorUtils {

  private record Error(@JsonIgnore Status status, String message, LocalDateTime timestamp) { }

  public static void send(ServerResponse res, Status status, String message) {
    var err = new Error(status, message, LocalDateTime.now());
    res.status(status).send(err);
  }

}
