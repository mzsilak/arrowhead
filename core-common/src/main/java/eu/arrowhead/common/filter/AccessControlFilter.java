/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.common.filter;

import eu.arrowhead.common.ArrowheadMain;
import eu.arrowhead.common.Utility;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.misc.SecurityUtils;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import org.apache.log4j.Logger;

public abstract class AccessControlFilter implements ContainerRequestFilter {

  protected static final Logger log = Logger.getLogger(AccessControlFilter.class.getName());

  protected Configuration configuration;

  // serverFields contains: coreSystemName, cloudName.operator.arrowhead.eu
  protected PrincipalSubjectData serverSubject;

  public AccessControlFilter(@Context Configuration configuration)
  {
    this.configuration = configuration;
    String serverCommonName = (String) configuration.getProperty("server_common_name");
    serverSubject = new PrincipalSubjectData(serverCommonName);
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    SecurityContext sc = requestContext.getSecurityContext();
    if (sc.isSecure()) {
      final PrincipalSubjectData clientSubject;
      final Principal principal;
      final URI requestTarget;
      final String requestJson;

      principal = sc.getUserPrincipal();
      requestTarget = requestContext.getUriInfo().getRequestUri();

      if (!isWhitelistedURI(requestTarget)) {
        clientSubject = new PrincipalSubjectData(principal);
        requestJson = Utility.getRequestPayload(requestContext.getEntityStream());
        verifyClientAuthorized(clientSubject, requestContext.getMethod(), requestTarget, requestJson);
        requestContext.setEntityStream(new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8)));
      }
    }
  }

  protected boolean isWhitelistedURI(final URI uri) {
    final String uriPath = uri.getPath();

    return uriPath.startsWith(ArrowheadMain.SWAGGER_PATH) || uriPath.equals(ArrowheadMain.OPENAPI_PATH);
  }

  protected void verifyNotAnonymous(final PrincipalSubjectData clientData, String method, URI requestTarget)
  {
    if (!clientData.isPresent()) {
      throwAccessDeniedException("Anonymous user", method, requestTarget.toString());
    }
  }

  protected void verifyClientAuthorized(final PrincipalSubjectData clientData, String method, URI requestTarget,
                                        String requestJson) {
    verifyNotAnonymous(clientData, method, requestTarget);

    //All requests from the local cloud are allowed
    if (!SecurityUtils.isKeyStoreCNArrowheadValid(clientData.getSubject(), serverSubject.getSuffix())) {
      throwAccessDeniedException(clientData.getCommonName(), method, requestTarget.toString());
    }
  }

  protected void verifyMatches(final PrincipalSubjectData client, final URI requestTarget,
                               final PrincipalSubjectData... validSystems)
  {
    boolean valid = false;

    for(PrincipalSubjectData system : validSystems)
    {
      valid |= system.equals(client);
    }
    if(!valid)
    {
      throwAccessDeniedException(client.getCommonName(), "", requestTarget.toString());
    }
  }

  protected void throwAccessDeniedException(final String user, final String method, final String url) throws AuthException {
    final String message = String.format("''%s'' is unauthorized to access %s''%s''", user, method, url);
    throwAuthException(message);
  }
  protected void throwAuthException(final String message) throws AuthException {
    log.error(message);
    throw new AuthException(message);
  }
}
