/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.gatekeeper;

import eu.arrowhead.common.DatabaseManager;
import eu.arrowhead.common.Utility;
import eu.arrowhead.common.database.ArrowheadSystem;
import eu.arrowhead.common.database.Broker;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.messages.ConnectToProviderRequest;
import eu.arrowhead.common.messages.ConnectToProviderResponse;
import eu.arrowhead.common.messages.GSDAnswer;
import eu.arrowhead.common.messages.GSDPoll;
import eu.arrowhead.common.messages.GatewayConnectionInfo;
import eu.arrowhead.common.messages.ICNEnd;
import eu.arrowhead.common.messages.ICNProposal;
import eu.arrowhead.common.messages.ICNResult;
import eu.arrowhead.common.messages.InterCloudAuthRequest;
import eu.arrowhead.common.messages.InterCloudAuthResponse;
import eu.arrowhead.common.messages.OrchestrationResponse;
import eu.arrowhead.common.messages.PreferredProvider;
import eu.arrowhead.common.messages.ServiceQueryForm;
import eu.arrowhead.common.messages.ServiceQueryResult;
import eu.arrowhead.common.messages.ServiceRequestForm;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import org.apache.log4j.Logger;

@Path(GatekeeperMain.GATEKEEPER_SERVICE_URI)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GatekeeperInboundResource {

  private static final Logger log = Logger.getLogger(GatekeeperInboundResource.class.getName());
  private static final DatabaseManager dm = DatabaseManager.getInstance();

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String getIt() {
    return "This is the inbound Gatekeeper Resource. Offering resources at: gsd_poll, icn_proposal.";
  }

  /**
   * This function represents the provider-side of GlobalServiceDiscovery, where the GateKeeper of the provider Cloud sends back its information if
   * the Authorization and Service Registry polling yields positive results.
   *
   * @return GSDAnswer
   */
  @PUT
  @Path("gsd_poll")
  public Response GSDPoll(@Valid GSDPoll gsdPoll) {
    // Polling the Authorization System about the consumer Cloud
    InterCloudAuthRequest authRequest = new InterCloudAuthRequest(gsdPoll.getRequesterCloud(), gsdPoll.getRequestedService());
    String authUri = UriBuilder.fromPath(GatekeeperMain.getAuthControlUri()).path("intercloud").toString();
    Response authResponse = Utility.sendRequest(authUri, "PUT", authRequest);

    // If the consumer Cloud is not authorized an error is returned
    if (!authResponse.readEntity(InterCloudAuthResponse.class).isAuthorized()) {
      log.info("GSD poll: Requester Cloud is UNAUTHORIZED, sending back error");
      throw new AuthException("Requester Cloud is UNAUTHORIZED to consume this service, GSD poll failed.", Status.UNAUTHORIZED.getStatusCode());
    }
    // If it is authorized, poll the Service Registry for the requested Service
    else {
      // Compiling the URI and the request payload
      String srUri = UriBuilder.fromPath(GatekeeperMain.SERVICE_REGISTRY_URI).path("query").toString();
      Map<String, Boolean> registryFlags = gsdPoll.getRegistryFlags();
      ServiceQueryForm queryForm = new ServiceQueryForm(gsdPoll.getRequestedService(), registryFlags.get("pingProviders"),
                                                        registryFlags.get("metadataSearch"));

      // Sending back provider Cloud information if the SR poll has results
      Response srResponse = Utility.sendRequest(srUri, "PUT", queryForm);
      ServiceQueryResult result = srResponse.readEntity(ServiceQueryResult.class);
      if (!result.isValid()) {
        log.info("GSD poll: SR query came back empty, sending back error");
        throw new DataNotFoundException("Service not found in the Service Registry, GSD poll failed.", Status.NOT_FOUND.getStatusCode());
      }

      log.info("GSDPoll successful, sending back GSDAnswer");
      GSDAnswer answer = new GSDAnswer(gsdPoll.getRequestedService(), Utility.getOwnCloud(GatekeeperMain.IS_SECURE));
      return Response.status(Status.OK).entity(answer).build();
    }
  }

  /**
   * This function represents the provider-side of InterCloudNegotiations, where the Gatekeeper (after an Orchestration process) sends information
   * about the provider System. (SSL secured)
   *
   * @return ICNEnd
   */
  @PUT
  @Path("icn_proposal")
  public Response ICNProposal(@Valid ICNProposal icnProposal) {
    // Polling the Authorization System about the consumer Cloud
    InterCloudAuthRequest authRequest = new InterCloudAuthRequest(icnProposal.getRequesterCloud(), icnProposal.getRequestedService());
    String authUri = UriBuilder.fromPath(GatekeeperMain.getAuthControlUri()).path("intercloud").toString();
    Response authResponse = Utility.sendRequest(authUri, "PUT", authRequest);

    // If the consumer Cloud is not authorized an error is returned
    if (!authResponse.readEntity(InterCloudAuthResponse.class).isAuthorized()) {
      log.info("ICNProposal: Requester Cloud is UNAUTHORIZED, sending back error");
      throw new AuthException("Requester Cloud is UNAUTHORIZED to consume this service, ICNProposal failed.", Status.UNAUTHORIZED.getStatusCode());
    }
    // If it is authorized, send a ServiceRequestForm to the Orchestrator and return the OrchestrationResponse
    Map<String, Boolean> orchestrationFlags = icnProposal.getNegotiationFlags();
    List<PreferredProvider> preferredProviders = new ArrayList<>();

    for (ArrowheadSystem preferredSystem : icnProposal.getPreferredSystems()) {
      preferredProviders.add(new PreferredProvider(preferredSystem, null));
    }

    // Changing the requesterSystem for the sake of proper token generation
    if (icnProposal.getNegotiationFlags().get("useGateway")) {
      if (!GatekeeperMain.USE_GATEWAY) {
        throw new ArrowheadException("The remote Gatekeeper is configured to use the Gateway Core System!");
      }
      icnProposal.getRequesterSystem().setSystemName(GatekeeperMain.getGatewayProviderUri()[1]);
    }
    ServiceRequestForm serviceRequestForm = new ServiceRequestForm.Builder(icnProposal.getRequesterSystem())
        .requesterCloud(icnProposal.getRequesterCloud()).requestedService(icnProposal.getRequestedService()).orchestrationFlags(orchestrationFlags)
        .preferredProviders(preferredProviders).build();

    Response response = Utility.sendRequest(GatekeeperMain.getOrchestratorUri(), "POST", serviceRequestForm);
    OrchestrationResponse orchResponse = response.readEntity(OrchestrationResponse.class);
    // If the gateway service is not requested, then return the full orchestration response
    if (!icnProposal.getNegotiationFlags().get("useGateway")) {
      ICNResult icnResult = new ICNResult(orchResponse);
      log.info("ICNProposal: returning the OrchestrationResponse to the requester Cloud.");
      return Response.status(response.getStatus()).entity(icnResult).build();
    }

    // Compiling the gateway request payload
    ArrowheadSystem provider = orchResponse.getResponse().get(0).getProvider();
    int timeout = icnProposal.getTimeout() > GatekeeperMain.TIMEOUT ? GatekeeperMain.TIMEOUT : icnProposal.getTimeout();

    // Getting the list of preferred brokers from database
    List<Broker> preferredBrokers = dm.getAll(Broker.class, null);

    // Filtering common brokers
    List<Broker> commonBrokers = new ArrayList<>(icnProposal.getPreferredBrokers());
    commonBrokers.retainAll(preferredBrokers);
    if (commonBrokers.size() == 0) {
      String security = GatekeeperMain.IS_SECURE ? "secure" : "insecure";
      throw new ArrowheadException("Could not find a common " + security + " broker, data path creation failed.");
    } else {
      //implement custom choosing algorithm here
      Broker chosenBroker = commonBrokers.get(0);
      log.debug("Common broker was chosen: " + chosenBroker.getAddress() + ":" + chosenBroker.getPort());

      ArrowheadSystem consumer = new ArrowheadSystem(icnProposal.getRequesterSystem());
      ConnectToProviderRequest connectionRequest = new ConnectToProviderRequest(chosenBroker.getAddress(), chosenBroker.getPort(), consumer, provider,
                                                                                icnProposal.getRequesterCloud(),
                                                                                Utility.getOwnCloud(GatekeeperMain.IS_SECURE),
                                                                                icnProposal.getRequestedService(), GatekeeperMain.IS_SECURE, timeout,
                                                                                icnProposal.getGatewayPublicKey());

      // Sending request, parsing response
      Response gatewayResponse = Utility.sendRequest(GatekeeperMain.getGatewayProviderUri()[0], "PUT", connectionRequest);
      ConnectToProviderResponse connectToProviderResponse = gatewayResponse.readEntity(ConnectToProviderResponse.class);

      GatewayConnectionInfo gatewayConnectionInfo = new GatewayConnectionInfo(chosenBroker.getAddress(), chosenBroker.getPort(),
                                                                              connectToProviderResponse.getQueueName(),
                                                                              connectToProviderResponse.getControlQueueName(),
                                                                              GatekeeperMain.getGatewayProviderUri()[3]);
      // The AMQP broker can only create 1 channel at the moment, so the gatekeeper have to choose an orchestration form
      ICNEnd icnEnd = new ICNEnd(orchResponse.getResponse().get(0), gatewayConnectionInfo);
      log.info("ICNProposal: returning the first OrchestrationForm and the GatewayConnectionInfo to the requester Cloud.");
      return Response.status(response.getStatus()).entity(icnEnd).build();
    }
  }
}
