package fr.esiee.app.errors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerResponse;

import java.time.LocalDateTime;

public class ErrorUtils {

  private static class Error {
    private final String message;
    @JsonIgnore
    private final Status status;
    private final LocalDateTime timestamp;

    public Error(Status status, String message) {
      this.message = message;
      this.status = status;
      this.timestamp = LocalDateTime.now();
    }

    public String getMessage() {
      return message;
    }

    public Status getStatus() {
      return status;
    }

    public int getStatusCode() {
      return status.code();
    }

    public String getStatusMessage() {
      return status.reasonPhrase();
    }

    public String getTimestamp() {
      return timestamp.toString();
    }
  }

  public static void send(ServerResponse res, Status status, String message) {
    var err = new Error(status, message);
    res.status(status).send(err);
  }
}
