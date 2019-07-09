/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.eventhandler.filter;

import eu.arrowhead.common.Utility;
import eu.arrowhead.common.database.EventFilter;
import eu.arrowhead.common.filter.AccessControlFilter;
import eu.arrowhead.common.filter.PrincipalSubjectData;
import eu.arrowhead.common.messages.PublishEvent;
import java.net.URI;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHORIZATION) //2nd highest priority constant, this filter gets executed after the SecurityFilter
public class EventHandlerACF extends AccessControlFilter {

  private final PrincipalSubjectData sysop;
  private final PrincipalSubjectData gatekeeper;

  public EventHandlerACF(@Context Configuration configuration) {
    super(configuration);
    sysop = serverSubject.createWithSuffix("sysop");
    gatekeeper = serverSubject.createWithSuffix("gatekeeper");
  }

  @Override
  public void verifyClientAuthorized(final PrincipalSubjectData clientData, String method, URI requestTarget,
                                     String requestJson) {

    verifyNotAnonymous(clientData, method, requestTarget);

    final String requestPath = requestTarget.getPath();

    if (requestPath.contains("mgmt")) {
      //Only the local System Operator can use these methods
      verifyMatches(clientData, requestTarget, sysop);
    } else {

      final String eventName;

      if (requestPath.contains("publish")) {
        PublishEvent event = Utility.fromJson(requestJson, PublishEvent.class);
        eventName = event.getSource().getSystemName();
      } else if (requestPath.contains("subscription")) {
        EventFilter filter = Utility.fromJson(requestJson, EventFilter.class);
        eventName = filter.getConsumer().getSystemName();
      } else {
        String[] uriParts = requestPath.split("/");
        eventName = uriParts[uriParts.length - 1];
      }

      if (!clientData.getCommonName().equalsIgnoreCase(eventName)) {
        throwAuthException(
          "JSON system name '" + eventName + "' and certificate common name '" + clientData.getCommonName()
            + "' do not " + "match.");
      }

      // Only requests from the local cloud are allowed
      if (!serverSubject.getSuffix().equals(clientData.getSubject())) {
        throwAccessDeniedException(clientData.getCommonName(), method, requestTarget.toString());
      }
    }
  }
}
