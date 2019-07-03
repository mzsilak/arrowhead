/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.systemregistry;

import eu.arrowhead.common.database.SystemRegistryEntry;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.BadPayloadException;
import eu.arrowhead.common.misc.registry_interfaces.RegistryResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.log4j.Logger;

/**
 * @author FHB
 */
@Path("systemregistry")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SystemRegistryResource implements RegistryResource<SystemRegistryEntry, Response> {

  private final Logger logger;
  private final SystemRegistryService registryService;

  public SystemRegistryResource() {
    logger = Logger.getLogger(SystemRegistryResource.class);
    registryService = new SystemRegistryService();
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public Response ping() {
    return Response.status(Response.Status.OK).entity("This is the System Registry Arrowhead Core System.").build();
  }

  @GET
  @Path(LOOKUP_PATH)
  @Operation(summary = "Searches a SystemRegistryEntry by id", responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = SystemRegistryEntry.class)))})
  public Response lookup(@PathParam("id") final long id) throws ArrowheadException {
    SystemRegistryEntry returnValue;
    Response response;

    logger.info("Lookup: " + id);
    returnValue = registryService.lookup(id);
    response = Response.status(Status.OK).entity(returnValue).build();

    return response;
  }

  @POST
  @Path(PUBLISH_PATH)
  public Response publish(@Valid final SystemRegistryEntry entry) throws ArrowheadException {
    SystemRegistryEntry returnValue;
    Response response;

    logger.info("publish: " + entry);

    if("0.0.0.0".equals(entry.getProvidedSystem().getAddress()))
    {
      throw new BadPayloadException("0.0.0.0 is not a valid destination IP address");
    }
    returnValue = registryService.publish(entry);
    response = Response.status(Status.CREATED).entity(returnValue).build();

    return response;
  }

  @POST
  @Path(UNPUBLISH_PATH)
  public Response unpublish(@Valid final SystemRegistryEntry entry) throws ArrowheadException {
    SystemRegistryEntry returnValue;
    Response response;

    logger.info("unpublish: " + entry);
    returnValue = registryService.unpublish(entry);
    response = Response.status(Status.OK).entity(returnValue).build();

    return response;
  }
}