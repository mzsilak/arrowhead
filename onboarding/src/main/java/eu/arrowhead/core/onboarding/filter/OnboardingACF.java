/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.onboarding.filter;

import eu.arrowhead.common.filter.AccessControlFilter;
import java.net.URI;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHORIZATION) //2nd highest priority constant, this filter gets executed after the SecurityFilter
public class OnboardingACF extends AccessControlFilter {

  public OnboardingACF(@Context Configuration configuration) {
    super(configuration);
  }

  @Override
  protected boolean isWhitelistedURI(URI uri) {
    return true;
  }
}
