/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.gatekeeper.filter;

import eu.arrowhead.common.filter.AccessControlFilter;
import eu.arrowhead.common.filter.PrincipalSubjectData;
import eu.arrowhead.common.filter.PrincipalSubjectData.SubjectFields;
import java.net.URI;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHORIZATION) //2nd highest priority constant, this filter gets executed after the SecurityFilter
public class GatekeeperACF extends AccessControlFilter {

  private PrincipalSubjectData sysop;
  private PrincipalSubjectData orchestrator;

  public GatekeeperACF(@Context Configuration configuration)
  {
    super(configuration);
    sysop = serverSubject.createWithSuffix("sysop");
    orchestrator = serverSubject.createWithSuffix("orchestrator");
  }

  @Override
  protected void verifyClientAuthorized(PrincipalSubjectData clientData, String method, URI requestTarget,
                                        String requestJson) {
    verifyNotAnonymous(clientData, method, requestTarget);

    final String requestPath = requestTarget.getPath();

    if (requestPath.contains("mgmt")) {
      //Only the local System Operator can use these methods
      verifyMatches(clientData, requestTarget, sysop);
    } else if (requestPath.endsWith("init_gsd") || requestPath.endsWith("init_icn")) {
      // Only requests from the local Orchestrator are allowed
      verifyMatches(clientData, requestTarget, orchestrator);
    }
    else {
      // Only requests from other Gatekeepers are allowed
      if(!clientData.equals(serverSubject, SubjectFields.COMMON_NAME, SubjectFields.ARROWHEAD))
      {
        throwAccessDeniedException(clientData.getCommonName(), method, requestTarget.toString());
      }
    }
  }
}
