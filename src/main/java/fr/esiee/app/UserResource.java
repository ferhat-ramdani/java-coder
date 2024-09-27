package fr.esiee.app;

import com.wordnik.swagger.annotations.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/user")
@Api(value="/user", description = "Operations about user")
@Produces({"application/json", "application/xml"})
public class UserResource {
  @POST
  @ApiOperation(value = "Create user",
          notes = "This can only be done by the logged in user.",
          position = 1)
  public Response createUser(
          @ApiParam(value = "Created user object", required = true) String user) {
    return Response.ok().entity("").build();
  }

  @POST
  @Path("/createWithArray")
  @ApiOperation(value = "Creates list of users with given input array",
          position = 2)
  public Response createUsersWithArrayInput(@ApiParam(value = "List of user object", required = true) String[] users) {
    return Response.ok().entity("").build();
  }

  @POST
  @Path("/createWithList")
  @ApiOperation(value = "Creates list of users with given input array",
          position = 3)
  public Response createUsersWithListInput(@ApiParam(value = "List of user object", required = true) java.util.List<String> users) {
    return Response.ok().entity("").build();
  }

  @GET
  @Path("/login")
  @ApiOperation(value = "Logs user into the system",
          response = String.class,
          position = 6)
  @ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid username/password supplied") })
  public Response loginUser(
          @ApiParam(value = "The user name for login", required = true) @QueryParam("username") String username,
          @ApiParam(value = "The password for login in clear text", required = true) @QueryParam("password") String password) {
    return Response.ok()
            .entity("logged in user session:" + System.currentTimeMillis())
            .build();
  }

  @GET
  @Path("/logout")
  @ApiOperation(value = "Logs out current logged in user session",
          position = 7)
  public Response logoutUser() {
    return Response.ok().entity("").build();
  }
}
