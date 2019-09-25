/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.common.messages;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class ICNResult {

  @Valid
  @NotNull
  private OrchestrationResponse orchResponse;

  public ICNResult() {
  }

  public ICNResult(OrchestrationResponse orchResponse) {
    this.orchResponse = orchResponse;
  }

  public OrchestrationResponse getOrchResponse() {
    return orchResponse;
  }

  public void setOrchResponse(OrchestrationResponse orchResponse) {
    this.orchResponse = orchResponse;
  }

  public boolean isValid() {
    return orchResponse != null && orchResponse.getResponse() != null && !orchResponse.getResponse().isEmpty();
  }

}
