package fr.esiee.app.exception;

import java.io.Serial;

public class RestApiException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = -5741898349056214368L;

  /**
   * Constructs a new RestApiException with the specified detail message.
   *
   * @param message the detail message
   */
  public RestApiException(String message) {
    super(message);
  }

  /**
   * Constructs a new RestApiException with the specified cause.
   *
   * @param cause the cause
   */
  public RestApiException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new RestApiException with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause   the cause
   */
  public RestApiException(String message, Throwable cause) {
    super(message, cause);
  }
}
