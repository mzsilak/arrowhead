/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.orchestrator.filter;

import eu.arrowhead.common.Utility;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.filter.AccessControlFilter;
import eu.arrowhead.common.filter.PrincipalSubjectData;
import eu.arrowhead.common.messages.ServiceRequestForm;
import java.net.URI;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHORIZATION) //2nd highest priority constant, this filter gets executed after the SecurityFilter
public class OrchestratorACF extends AccessControlFilter {

  private PrincipalSubjectData sysop;
  private PrincipalSubjectData gatekeeper;


  protected OrchestratorACF(@Context Configuration configuration) {
    super(configuration);
    sysop = serverSubject.createWithSuffix("sysop");
    gatekeeper = serverSubject.createWithSuffix("gatekeeper");
  }

  @Override
  protected void verifyClientAuthorized(PrincipalSubjectData clientData, String method, URI requestTarget,
                                        String requestJson) {
    verifyNotAnonymous(clientData, method, requestTarget);

    final String requestPath = requestTarget.getPath();

    if (requestPath.contains("mgmt")) {
      //Only the local System Operator can use these methods
      verifyMatches(clientData, requestTarget, sysop);
    } else if (requestPath.contains("store")) {
      // Only requests from the local cloud are allowed
      if (!serverSubject.getSuffix().equals(clientData.getSubject())) {
        throwAccessDeniedException(clientData.getCommonName(), method, requestTarget.toString());
      }
    }

    ServiceRequestForm srf = Utility.fromJson(requestJson, ServiceRequestForm.class);

    // If this is an external service request, only the local Gatekeeper can send this method
    if (srf.getOrchestrationFlags().getOrDefault("externalServiceRequest", false)) {
      verifyMatches(clientData, requestTarget, gatekeeper);
    } else {
      // Otherwise all request from the local cloud are allowed if the consumer name and service name match
      String consumerName = srf.getRequesterSystem().getSystemName();

      if (!clientData.getCommonName().equalsIgnoreCase(consumerName) && !clientData.getCommonName().equalsIgnoreCase(
        consumerName.replaceAll("_", ""))) {
        log.error("Requester system name and cert common name do not match!");
        throw new AuthException(
          "Requester system " + srf.getRequesterSystem().getSystemName() + " and cert common name (" + clientData
            .getCommonName() + ") do not match!");
      }
    }
  }
}
