/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.gateway.filter;

import eu.arrowhead.common.filter.AccessControlFilter;
import eu.arrowhead.common.filter.PrincipalSubjectData;
import java.net.URI;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHORIZATION) //2nd highest priority constant, this filter gets executed after the SecurityFilter
public class GatewayACF extends AccessControlFilter {

  private PrincipalSubjectData sysop;
  private PrincipalSubjectData gatekeeper;

  protected GatewayACF(@Context Configuration configuration)
  {
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
    }

    verifyMatches(clientData, requestTarget, gatekeeper);
  }
}
