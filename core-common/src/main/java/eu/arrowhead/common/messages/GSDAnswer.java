/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.common.messages;

import eu.arrowhead.common.database.ArrowheadCloud;
import eu.arrowhead.common.database.ArrowheadService;
import javax.validation.Valid;

public class GSDAnswer {

  @Valid
  private ArrowheadService requestedService;
  @Valid
  private ArrowheadCloud providerCloud;

  public GSDAnswer() {
  }

  public GSDAnswer(ArrowheadService requestedService, ArrowheadCloud providerCloud) {
    this.requestedService = requestedService;
    this.providerCloud = providerCloud;
  }

  public ArrowheadService getRequestedService() {
    return requestedService;
  }

  public void setRequestedService(ArrowheadService requestedService) {
    this.requestedService = requestedService;
  }

  public ArrowheadCloud getProviderCloud() {
    return providerCloud;
  }

  public void setProviderCloud(ArrowheadCloud providerCloud) {
    this.providerCloud = providerCloud;
  }

}
