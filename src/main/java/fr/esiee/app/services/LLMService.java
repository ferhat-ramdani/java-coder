package fr.esiee.app.services;

import fr.esiee.app.db.DbManager;
import fr.esiee.app.exception.RestApiException;
import fr.esiee.app.llms.LLMDTO;
import io.helidon.common.context.Contexts;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.NoSuchElementException;

/**
 * Class representing the LLMService.
 * Provides endpoints to get LLM's information.
 */
@Tag(name = "LLM", description = "Endpoints to get llm's informations.")
@ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = LLMDTO.class)), responseCode = "200", description = "Successful operation")
@Path("/api/llm/")
public class LLMService implements HttpService {

  private final DbManager dbService;

  /**
   * Constructs a new LLMService instance.
   * Initializes the DbManager from the global context.
   *
   * @throws NoSuchElementException if the DbManager is not found in the global context
   */
  public LLMService() {
    dbService = Contexts.globalContext().get(DbManager.class)
            .orElseThrow(() -> new NoSuchElementException("DbManager not found."));
  }

  /**
   * Configures the routing rules for the HTTP service.
   *
   * @param httpRules the HTTP rules to configure
   */
  @Override
  public void routing(HttpRules httpRules) {
    httpRules.get("/", this::getListOfLLM)
            .get("/{id}", this::getLLMById)
            .get("/first/llm", this::getFirstLLM);
  }

  /**
   * Endpoint to get the list of all LLMs.
   *
   * @param req the server request
   * @param res the server response
   */
  @GET
  @javax.ws.rs.Path("/")
  @Operation(summary = "get LLMs", description = "Get list of all LLMs")
  @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = LLMDTO.class))))
  public void getListOfLLM(@Parameter(hidden = true) ServerRequest req, @Parameter(hidden = true) ServerResponse res) {
    var llmsToSend = dbService.listLLMs().stream().map(LLMDTO::copyOf).toList();
    res.status(Status.OK_200).send(llmsToSend);
  }

  /**
   * Endpoint to get the first LLM.
   *
   * @param req the server request
   * @param res the server response
   */
  @GET
  @javax.ws.rs.Path("/first")
  @Operation(summary = "get first LLM", description = "Get the first LLM of the list of supported LLMs")
  public void getFirstLLM(@Parameter(hidden = true) ServerRequest req, @Parameter(hidden = true) ServerResponse res) {
    var llm = dbService.getFirstLLM();
    res.status(Status.OK_200).send(LLMDTO.copyOf(llm));
  }

  /**
   * Retrieves a LLM by its ID.
   *
   * @param req the server request containing the LLM ID
   * @param res the server response to send the LLM data
   * @throws RestApiException if the LLM ID is not provided
   */
  @GET
  @javax.ws.rs.Path("/{id}")
  @Operation(summary = "get LLM by ID", description = "Get a LLM by its ID")
  public void getLLMById(
          @Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the llm to retrieve", required = true, schema = @Schema(type = "integer", description = "Chat ID", example = "1"))
          ServerRequest req,
          @Parameter(hidden = true) ServerResponse res
  ) {
    int llmId = req.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new RestApiException("LLM id is required"));
    var llm = dbService.getLLMById(llmId);
    res.status(Status.OK_200).send(LLMDTO.copyOf(llm));
  }
}
