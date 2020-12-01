/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.serviceregistry_sql.filter;

import eu.arrowhead.common.Utility;
import eu.arrowhead.common.database.ServiceRegistryEntry;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.filter.AccessControlFilter;
import eu.arrowhead.common.filter.PrincipalSubjectData;
import eu.arrowhead.common.filter.PrincipalSubjectData.SubjectFields;
import java.net.URI;
import java.util.Objects;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHORIZATION) //2nd highest priority constant, this filter gets executed after the SecurityFilter
public class ServiceRegACF extends AccessControlFilter {

  private PrincipalSubjectData gatekeeper;
  private PrincipalSubjectData orchestrator;
  private PrincipalSubjectData certificateAuthority;
  private PrincipalSubjectData onboardingController;

  public ServiceRegACF(@Context Configuration configuration)
  {
    super(configuration);
    gatekeeper = serverSubject.createWithPrefix("gatekeeper");
    orchestrator = serverSubject.createWithPrefix("orchestrator");
    certificateAuthority = serverSubject.createWithPrefix("certificateauthority");
    onboardingController = serverSubject.createWithPrefix("onboarding");
  }

  @Override
  protected void verifyClientAuthorized(PrincipalSubjectData clientSubject, String method, URI requestTarget,
                                        String requestJson) {

    final String path = requestTarget.getPath();

    if(path.endsWith("query"))
    {
      verifyMatches(clientSubject, requestTarget, gatekeeper, orchestrator, certificateAuthority, onboardingController);
    }
    else if(path.endsWith("register") || path.endsWith("remove"))
    {
      // may only register/remove its own service
      final ServiceRegistryEntry entry = Utility.fromJson(requestJson, ServiceRegistryEntry.class);

      if(Objects.isNull(entry.getProvider()) || Objects.isNull(entry.getProvider().getSystemName()))
      {
        throw new AuthException("ProviderName is missing");
      }

      final String providerName = entry.getProvider().getSystemName();

      if(!clientSubject.isPresent())
      {
        throw new AuthException("Client certificate is missing!");
      }


      if (!providerName.equalsIgnoreCase(clientSubject.getCommonName())) {
        log.error("Provider system name and cert common name do not match! SR registering/removing denied!");
        throw new AuthException("Provider system " + providerName + " and cert common name (" + clientSubject.getCommonName() + ") do not match!");
      }
    }
    // All requests from the local cloud are allowed
    else if(!clientSubject.equals(serverSubject, SubjectFields.ARROWHEAD, SubjectFields.OPERATOR, SubjectFields.CLOUD_NAME))
    {
      throwAccessDeniedException(clientSubject.getCommonName(), method, requestTarget.toString());
    }
  }}
