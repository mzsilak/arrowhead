/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.eventhandler;

import eu.arrowhead.common.Utility;
import eu.arrowhead.common.database.EventFilter;
import eu.arrowhead.common.exception.BadPayloadException;
import eu.arrowhead.common.messages.PublishEvent;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is the REST resource for the Event Handler Core System.
 */
@Path("eventhandler")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EventHandlerResource {

  private static final Logger log = LogManager.getLogger(EventHandlerResource.class.getName());

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String getIt() {
    return "This is the Event Handler Arrowhead Core System.";
  }

  @POST
  @Path("publish")
  public Response publishEvent(@Valid PublishEvent eventPublished, @Context ContainerRequestContext requestContext) {
    if (eventPublished.getEvent().getTimestamp() == null) {
      eventPublished.getEvent().setTimestamp(ZonedDateTime.now());
    }
    if (EventHandlerMain.EVENT_PUBLISHING_TOLERANCE > 0) {
      if (eventPublished.getEvent().getTimestamp().isBefore(ZonedDateTime.now().minusMinutes(EventHandlerMain.EVENT_PUBLISHING_TOLERANCE))) {
        throw new BadPayloadException(
            "This event is too old to publish. Maximum allowed delay before publishing the event: " + EventHandlerMain.EVENT_PUBLISHING_TOLERANCE);
      }
      if (eventPublished.getEvent().getTimestamp()
                        .isAfter(ZonedDateTime.now().plusMinutes(EventHandlerMain.EVENT_PUBLISHING_TOLERANCE))) {
        throw new BadPayloadException(
            "This event is too far in the future. Maximum allowed timestamp tolerance for events: " + EventHandlerMain.EVENT_PUBLISHING_TOLERANCE);
      }
    }
    boolean isSecure = requestContext.getSecurityContext().isSecure();

    /* First the event will be propagated to consumers, then the results will be sent back to the publisher, summarizing which consumers received the
       event without an error. */
    CompletableFuture.supplyAsync(() -> EventHandlerService.propagateEvent(eventPublished)).thenAccept(map -> {
      if (eventPublished.getDeliveryCompleteUri() != null) {
        String callbackUrl = Utility
            .getUri(eventPublished.getSource().getAddress(), eventPublished.getSource().getPort(), eventPublished.getDeliveryCompleteUri(), isSecure,
                    false);
        try {
          Utility.sendRequest(callbackUrl, "POST", map);
        } catch (RuntimeException e) {
          log.error("Callback after event publishing failed at: " + callbackUrl);
          e.printStackTrace();
        }
      }
    });

    //return OK while the event publishing happens in async
    return Response.status(Status.OK).build();
  }

  @POST
  @Path("subscription")
  public Response subscribe(@Valid EventFilter filter) {
    EventFilter savedFilter = EventHandlerService.saveEventFilter(filter);
    if (savedFilter != null) {
      log.info("EventFilter was saved.");
      return Response.status(Status.CREATED.getStatusCode()).entity(savedFilter).build();
    } else {
      log.info("EventFilter was already in the database, nothing happened.");
      return Response.status(Status.NO_CONTENT.getStatusCode()).build();
    }
  }

  @DELETE
  @Path("subscription/type/{eventType}/consumer/{consumerName}")
  public Response unsubscribe(@PathParam("eventType") String eventType, @PathParam("consumerName") String consumerName) {
    int statusCode = EventHandlerService.deleteEventFilter(eventType, consumerName);
    log.info("deleteEventFilter returned with status code: " + statusCode);
    return Response.status(statusCode).build();
  }

  @PUT
  @Path("subscription")
  public Response unsubscribe(@Valid EventFilter filter) {
    int statusCode = EventHandlerService.deleteEventFilter(filter.getEventType(), filter.getConsumer().getSystemName());
    log.info("deleteEventFilter returned with status code: " + statusCode);
    return Response.status(statusCode).build();
  }

}
