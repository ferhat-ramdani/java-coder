package fr.esiee.app.services;

import fr.esiee.app.dto.LLMDTO;
import io.helidon.common.context.Contexts;
import io.helidon.http.Status;
import io.helidon.webserver.http.*;
import io.helidon.http.BadRequestException;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@OpenAPIDefinition(
        info = @Info(
                title = "GPT for dev API services",
                description = "Services for manipulating chats, prompts and llms"
        ),
        servers = {
                @Server(
                        description = "localhost",
                        url = "http://localhost:8080")
        }
)
@Tag(name = "LLM", description = "endpoints to use llm")
@Path("/api/llm/")
public class LLMService implements HttpService {

  private final DbService dbService;

  public LLMService() {
    dbService = Contexts.globalContext().get(DbService.class).orElse(DbService.getInstance());
  }

  @Override
  public void routing(HttpRules httpRules) {
    httpRules.get("/", this::getLLM)
            .get("/{id}", this::getLLMById);
  }

  @GET
  @javax.ws.rs.Path("/")
  @Operation(summary = "get LLMs", description = "Get list of all LLMs")
  public void getLLM(@Parameter(hidden = true) ServerRequest req, @Parameter(hidden = true) ServerResponse res) {
    var llmToSend = dbService.listLLMs().stream().map(e -> new LLMDTO(e.id(),e.name(),e.model(), e.caracteristics())).toList();
    res.status(Status.OK_200).send(llmToSend);
  }

  @GET
  @javax.ws.rs.Path("/{id}")
  @Operation(summary = "get LLMs", description = "Get list of all LLMs")
  public void getLLMById(
          @Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the llm to retrieve", required = true, schema = @Schema(type = "integer", description = "Chat ID", example = "1")) ServerRequest req,
          @Parameter(hidden = true) ServerResponse res
  ) {
    int llmId = req.path().pathParameters().first("id").map(Integer::parseInt)
            .orElseThrow(() -> new BadRequestException("LLM id is required"));
    var llm = dbService.getLLMById(llmId);
    var llmDTO = new LLMDTO(llm.id(),llm.name(),llm.model(), llm.caracteristics());
    res.send(llmDTO);
  }
}
