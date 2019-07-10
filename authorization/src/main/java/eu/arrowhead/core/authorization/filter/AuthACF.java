/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.authorization.filter;

import eu.arrowhead.common.filter.AccessControlFilter;
import eu.arrowhead.common.filter.PrincipalSubjectData;
import eu.arrowhead.core.authorization.AuthorizationMain;
import java.net.URI;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHORIZATION) //2nd highest priority constant, this filter gets executed after the SecurityFilter
public class AuthACF extends AccessControlFilter {

  private PrincipalSubjectData sysop;
  private PrincipalSubjectData orchestrator;
  private PrincipalSubjectData gatekeeper;
  private PrincipalSubjectData certificateAuthority;

  public AuthACF(@Context Configuration configuration)
  {
    super(configuration);
    sysop = serverSubject.createWithSuffix("sysop");
    orchestrator = serverSubject.createWithSuffix("orchestrator");
    gatekeeper = serverSubject.createWithSuffix("gatekeeper");
    certificateAuthority = serverSubject.createWithSuffix("certificateauthority");
  }

  @Override
  protected void verifyClientAuthorized(PrincipalSubjectData clientData, String method, URI requestTarget,
                                        String requestJson) {
    verifyNotAnonymous(clientData, method, requestTarget);

    final String requestPath = requestTarget.getPath();

    if (requestPath.contains("mgmt")) {
      //Only the local System Operator can use these methods
      verifyMgmtAccess(clientData, method, requestTarget);
    } else if (AuthorizationMain.enableAuthForCloud && requestPath.endsWith("intracloud")) {
      verifyIntracloudAccess(clientData, method, requestTarget);
    } else {
      verifyAuthorizedSystems(clientData, method, requestTarget);
    }
  }

  private void verifyAuthorizedSystems(PrincipalSubjectData clientData, String method, URI requestTarget) {
    verifyMatches(clientData, requestTarget, orchestrator, gatekeeper);
  }

  private void verifyIntracloudAccess(PrincipalSubjectData clientData, String method, URI requestTarget) {
    final String requestPath = requestTarget.getPath();
    if (!(requestPath.endsWith("intracloud") && method.equalsIgnoreCase("POST"))) {
      throwAccessDeniedException(clientData.getCommonName(), method, requestPath);
    }
  }

  private void verifyMgmtAccess(PrincipalSubjectData clientData, String method, URI requestTarget) {
    verifyMatches(clientData, requestTarget, sysop, certificateAuthority);

    if (clientData.equals(certificateAuthority)) {
      if (!(requestTarget.getPath().endsWith("publickey") && method.equalsIgnoreCase("GET"))) {
        throwAccessDeniedException(clientData.getCommonName(), method, requestTarget.getPath());
      }
    }
  }
}
