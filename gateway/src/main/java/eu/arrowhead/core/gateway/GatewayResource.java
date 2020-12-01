/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.gateway;

import eu.arrowhead.common.messages.ConnectToConsumerRequest;
import eu.arrowhead.common.messages.ConnectToConsumerResponse;
import eu.arrowhead.common.messages.ConnectToProviderRequest;
import eu.arrowhead.common.messages.ConnectToProviderResponse;
import eu.arrowhead.core.gateway.model.ActiveSession;
import eu.arrowhead.core.gateway.model.GatewaySession;
import eu.arrowhead.core.gateway.thread.InsecureServerSocketThread;
import eu.arrowhead.core.gateway.thread.InsecureSocketThread;
import eu.arrowhead.core.gateway.thread.SecureServerSocketThread;
import eu.arrowhead.core.gateway.thread.SecureSocketThread;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is the REST resource for the Gateway Core System.
 */
@Path("gateway")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GatewayResource {

  private static final Logger log = LogManager.getLogger(GatewayResource.class.getName());

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String getIt() {
    return "This is the Gateway Resource. REST methods: connectToProvider, connectToConsumer.";
  }

  @GET
  @Path("management")
  public List<ActiveSession> sessionManagement() {
    return new ArrayList<>(GatewayService.activeSessions.values());
  }

  @PUT
  @Path("connectToProvider")
  public Response connectToProvider(ConnectToProviderRequest connectionRequest) {
    String queueName = String.valueOf(System.currentTimeMillis()).concat(String.valueOf(Math.random())).replace(".", "");
    String controlQueueName = queueName.concat("_control");

    ActiveSession activeSession = new ActiveSession(connectionRequest.getConsumer(), connectionRequest.getConsumerCloud(),
                                                    connectionRequest.getProvider(), connectionRequest.getProviderCloud(),
                                                    connectionRequest.getService(), connectionRequest.getBrokerHost(),
                                                    connectionRequest.getBrokerPort(), null, queueName, controlQueueName,
                                                    connectionRequest.getIsSecure(), LocalDateTime.now());
    // Add the session to the management queue
    GatewayService.activeSessions.put(queueName, activeSession);

    GatewaySession gatewaySession = GatewayService
        .createChannel(connectionRequest.getBrokerHost(), connectionRequest.getBrokerPort(), queueName, controlQueueName,
                       connectionRequest.getIsSecure());

    if (connectionRequest.getIsSecure()) {
      SecureSocketThread secureThread = new SecureSocketThread(gatewaySession, queueName, controlQueueName, connectionRequest);
      secureThread.start();
    } else {
      InsecureSocketThread insecureThread = new InsecureSocketThread(gatewaySession, queueName, controlQueueName, connectionRequest);
      insecureThread.start();
    }

    ConnectToProviderResponse response = new ConnectToProviderResponse(queueName, controlQueueName);
    log.info("Returning the ConnectToProviderResponse to the Gatekeeper");
    return Response.status(200).entity(response).build();
  }

  @PUT
  @Path("connectToConsumer")
  public Response connectToConsumer(ConnectToConsumerRequest connectionRequest) {
    Integer serverSocketPort = GatewayService.getAvailablePort();

    ActiveSession activeSession = new ActiveSession(connectionRequest.getConsumer(), connectionRequest.getConsumerCloud(),
                                                    connectionRequest.getProvider(), connectionRequest.getProviderCloud(),
                                                    connectionRequest.getService(), connectionRequest.getBrokerHost(),
                                                    connectionRequest.getBrokerPort(), serverSocketPort, connectionRequest.getQueueName(),
                                                    connectionRequest.getControlQueueName(), connectionRequest.getIsSecure(), LocalDateTime.now());
    // Add the session to the management queue
    GatewayService.activeSessions.put(connectionRequest.getQueueName(), activeSession);

    GatewaySession gatewaySession = GatewayService
        .createChannel(connectionRequest.getBrokerHost(), connectionRequest.getBrokerPort(), connectionRequest.getQueueName(),
                       connectionRequest.getControlQueueName(), connectionRequest.getIsSecure());

    if (connectionRequest.getIsSecure()) {
      SecureServerSocketThread secureThread = new SecureServerSocketThread(gatewaySession, serverSocketPort, connectionRequest);
      secureThread.start();
    } else {
      InsecureServerSocketThread insecureThread = new InsecureServerSocketThread(gatewaySession, serverSocketPort, connectionRequest);
      insecureThread.start();
    }

    ConnectToConsumerResponse response = new ConnectToConsumerResponse(serverSocketPort);
    log.info("Returning the ConnectToConsumerResponse to the Gatekeeper");
    return Response.status(200).entity(response).build();
  }

}
