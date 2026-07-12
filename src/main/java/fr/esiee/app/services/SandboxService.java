package fr.esiee.app.services;

import fr.esiee.app.db.DbManager;
import fr.esiee.app.exception.RestApiException;
import fr.esiee.app.sandbox.SandboxSessionManager;
import fr.esiee.app.utils.ErrorUtils;
import io.helidon.common.context.Contexts;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.sse.SseSink;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * Endpoints to run a generated class inside an isolated, interactive Docker sandbox, as if it
 * were a real terminal: start a session, stream its output live, send it stdin, and stop it.
 */
@Tag(name = "Sandbox", description = "endpoints to run generated code in an isolated Docker sandbox")
@Path("/api/sandbox")
public class SandboxService implements HttpService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SandboxService.class);

  private final DbManager dbService;

  public record SessionId(String sessionId) {
  }

  public record InputPayload(String input) {
  }

  /**
   * Constructs a new SandboxService instance, wiring up the DbManager from the global context.
   *
   * @throws NoSuchElementException if the DbManager is not found in the global context
   */
  public SandboxService() {
    this.dbService = Contexts.globalContext().get(DbManager.class)
            .orElseThrow(() -> new NoSuchElementException("DbManager not found."));
  }

  @Override
  public void routing(HttpRules rules) {
    rules.post("/exec/{promptId}", this::startSession)
            .get("/exec/{sessionId}/stream", this::streamSession)
            .post("/exec/{sessionId}/input", this::sendInput)
            .delete("/exec/{sessionId}", this::stopSession);
  }

  /**
   * Starts a new interactive sandbox session running the code stored in the given prompt.
   *
   * @param req the server request
   * @param res the server response, will contain the new session's id
   */
  @POST
  @Path("/exec/{promptId}")
  @Operation(summary = "start a sandboxed execution session", description = "Compiles and runs a prompt's code inside an isolated Docker container")
  @ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = SessionId.class)),
          responseCode = "200", description = "Session started")
  public void startSession(
          @Parameter(name = "promptId", in = ParameterIn.PATH, required = true, schema = @Schema(type = "integer"))
          ServerRequest req,
          @Parameter(hidden = true) ServerResponse res) {
    int promptId = req.path().pathParameters().first("promptId").map(Integer::parseInt)
            .orElseThrow(() -> new RestApiException("Prompt ID is required"));
    var prompt = dbService.getPromptById(promptId);
    if (!prompt.compile()) {
      LOGGER.error("Front is trying to execute a class that does not compile, promptId : {}", promptId);
      ErrorUtils.send(res, Status.NOT_ACCEPTABLE_406, "This code did not compile successfully and cannot be run.");
      return;
    }
    try {
      LOGGER.info("Starting sandbox session, promptId : {}", promptId);
      var session = SandboxSessionManager.instance().start(prompt.message());
      res.status(Status.OK_200).send(new SessionId(session.id()));
    } catch (IOException e) {
      ErrorUtils.send(res, Status.INTERNAL_SERVER_ERROR_500, e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RestApiException(e);
    }
  }

  /**
   * Streams a running session's output and lifecycle events over SSE. Attaching after the
   * session has already produced output replays everything so far, then continues live.
   *
   * @param req the server request
   * @param res the server response
   */
  @GET
  @Path("/exec/{sessionId}/stream")
  @Operation(summary = "stream sandbox session output", description = "Streams stdout and lifecycle events for a running sandbox session")
  public void streamSession(
          @Parameter(name = "sessionId", in = ParameterIn.PATH, required = true)
          ServerRequest req,
          @Parameter(hidden = true) ServerResponse res) {
    var sessionId = req.path().pathParameters().first("sessionId")
            .orElseThrow(() -> new RestApiException("Session ID is required"));
    var session = SandboxSessionManager.instance().get(sessionId);
    if (session == null) {
      ErrorUtils.send(res, Status.NOT_FOUND_404, "Sandbox session not found or already finished.");
      return;
    }
    try (var sseSink = res.sink(SseSink.TYPE)) {
      session.attach(sseSink);
      try {
        session.awaitFinished();
      } finally {
        session.detach(sseSink);
      }
    }
  }

  /**
   * Sends a line of input to a running session, as if a user had typed it at a terminal.
   *
   * @param req the server request
   * @param res the server response
   */
  @POST
  @Path("/exec/{sessionId}/input")
  @Consumes("application/json")
  @Operation(summary = "send input to a sandbox session", description = "Writes a line of input to the running program's stdin")
  public void sendInput(
          @Parameter(name = "sessionId", in = ParameterIn.PATH, required = true)
          ServerRequest req,
          @Parameter(hidden = true) ServerResponse res) {
    var sessionId = req.path().pathParameters().first("sessionId")
            .orElseThrow(() -> new RestApiException("Session ID is required"));
    var session = SandboxSessionManager.instance().get(sessionId);
    if (session == null) {
      ErrorUtils.send(res, Status.NOT_FOUND_404, "Sandbox session not found or already finished.");
      return;
    }
    var payload = req.content().as(InputPayload.class);
    try {
      session.sendInput(payload.input());
      res.status(Status.OK_200).send();
    } catch (IOException e) {
      ErrorUtils.send(res, Status.CONFLICT_409, e.getMessage());
    }
  }

  /**
   * Stops a running session and tears down its container.
   *
   * @param req the server request
   * @param res the server response
   */
  @DELETE
  @Path("/exec/{sessionId}")
  @Operation(summary = "stop a sandbox session", description = "Terminates a running sandbox session and removes its container")
  public void stopSession(
          @Parameter(name = "sessionId", in = ParameterIn.PATH, required = true)
          ServerRequest req,
          @Parameter(hidden = true) ServerResponse res) {
    var sessionId = req.path().pathParameters().first("sessionId")
            .orElseThrow(() -> new RestApiException("Session ID is required"));
    SandboxSessionManager.instance().remove(sessionId);
    res.status(Status.OK_200).send();
  }
}
