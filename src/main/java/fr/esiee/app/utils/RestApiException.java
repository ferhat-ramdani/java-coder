package fr.esiee.app.exception;

public class RestApiException extends RuntimeException {
  public RestApiException(String message) {
    super(message);
  }

  public RestApiException(Throwable cause) {
    super(cause);
  }

  public RestApiException() {
    super();
  }

  public RestApiException(String message, Throwable cause) {
    super(message, cause);
  }
}
