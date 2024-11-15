package fr.esiee.app.exception;

import java.io.Serial;

public class RestApiException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = -5741898349056214368L;

  public RestApiException(String message) {
    super(message);
  }

  public RestApiException(Throwable cause) {
    super(cause);
  }

  public RestApiException(String message, Throwable cause) {
    super(message, cause);
  }
}
