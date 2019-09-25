/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.common.messages;

import com.fasterxml.jackson.annotation.JsonSetter;
import eu.arrowhead.common.database.ArrowheadCloud;
import eu.arrowhead.common.database.ArrowheadService;
import eu.arrowhead.common.database.ArrowheadSystem;
import eu.arrowhead.common.exception.BadPayloadException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * This is what the Orchestrator Core System receives from Arrowhead Systems trying to request services.
 */
public class ServiceRequestForm {

  private static final List<String> flagKeys = new ArrayList<>(Arrays.asList("triggerInterCloud", "externalServiceRequest", "enableInterCloud",
                                                                             "metadataSearch", "pingProviders", "overrideStore", "matchmaking",
                                                                             "onlyPreferred", "enableQoS"));
  @Valid
  @NotNull
  private ArrowheadSystem requesterSystem;
  @Valid
  private ArrowheadCloud requesterCloud;
  @Valid
  private ArrowheadService requestedService;
  @Size(max = 9, message = "There are only 9 orchestration flags, map size must not be bigger than 9")
  private Map<String, Boolean> orchestrationFlags = new HashMap<>();
  @Valid
  private List<PreferredProvider> preferredProviders = new ArrayList<>();
  private Map<String, String> requestedQoS = new HashMap<>();
  private Map<String, String> commands = new HashMap<>();

  public ServiceRequestForm() {
    for (String key : flagKeys) {
      if (!orchestrationFlags.containsKey(key)) {
        orchestrationFlags.put(key, false);
      }
    }
  }

  private ServiceRequestForm(Builder builder) {
    requesterSystem = builder.requesterSystem;
    requesterCloud = builder.requesterCloud;
    requestedService = builder.requestedService;
    orchestrationFlags = builder.orchestrationFlags;
    preferredProviders = builder.preferredProviders;
    requestedQoS = builder.requestedQoS;
    commands = builder.commands;
  }

  public ArrowheadSystem getRequesterSystem() {
    return requesterSystem;
  }

  public void setRequesterSystem(ArrowheadSystem requesterSystem) {
    this.requesterSystem = requesterSystem;
  }

  public ArrowheadCloud getRequesterCloud() {
    return requesterCloud;
  }

  public void setRequesterCloud(ArrowheadCloud requesterCloud) {
    this.requesterCloud = requesterCloud;
  }

  public ArrowheadService getRequestedService() {
    return requestedService;
  }

  public void setRequestedService(ArrowheadService requestedService) {
    this.requestedService = requestedService;
  }


  public Map<String, Boolean> getOrchestrationFlags() {
    return orchestrationFlags;
  }

  @JsonSetter
  public void setOrchestrationFlags(Map<String, Boolean> orchestrationFlags) {
    for (Map.Entry<String, Boolean> entry : orchestrationFlags.entrySet()) {
      String key = entry.getKey();
      if (key == null || key.trim().isEmpty()) {
        throw new BadPayloadException("SRF orchestration flag key can not be blank!");
      }
      Boolean value = entry.getValue();
      if (value == null) {
        throw new BadPayloadException("SRF orchestration flag value can not be null!");
      }
    }
    this.orchestrationFlags = orchestrationFlags;
  }

  public List<PreferredProvider> getPreferredProviders() {
    return preferredProviders;
  }

  public void setPreferredProviders(List<PreferredProvider> preferredProviders) {
    this.preferredProviders = preferredProviders;
  }

  public Map<String, String> getRequestedQoS() {
    return requestedQoS;
  }

  public void setRequestedQoS(Map<String, String> requestedQoS) {
    this.requestedQoS = requestedQoS;
  }

  public Map<String, String> getCommands() {
    return commands;
  }

  public void setCommands(Map<String, String> commands) {
    this.commands = commands;
  }

  public static class Builder {

    // Required parameters
    private ArrowheadSystem requesterSystem;
    // Optional parameters
    private ArrowheadCloud requesterCloud;
    private ArrowheadService requestedService;

    private Map<String, Boolean> orchestrationFlags = new HashMap<>();
    private List<PreferredProvider> preferredProviders = new ArrayList<>();
    private Map<String, String> requestedQoS = new HashMap<>();
    private Map<String, String> commands = new HashMap<>();

    public Builder(ArrowheadSystem requesterSystem) {
      this.requesterSystem = requesterSystem;
    }

    public Builder requesterCloud(ArrowheadCloud cloud) {
      requesterCloud = cloud;
      return this;
    }

    public Builder requestedService(ArrowheadService service) {
      requestedService = service;
      return this;
    }

    public Builder orchestrationFlags(Map<String, Boolean> flags) {
      orchestrationFlags = flags;
      return this;
    }

    public Builder preferredProviders(List<PreferredProvider> providers) {
      preferredProviders = providers;
      return this;
    }

    public Builder requestedQoS(Map<String, String> qos) {
      requestedQoS = qos;
      return this;
    }

    public Builder commands(Map<String, String> commands) {
      this.commands = commands;
      return this;
    }

    public ServiceRequestForm build() {
      return new ServiceRequestForm(this);
    }
  }

  public void validateCrossParameterConstraints() {
    for (String key : flagKeys) {
      if (!orchestrationFlags.containsKey(key)) {
        orchestrationFlags.put(key, false);
      }
    }
    if (requestedService == null && orchestrationFlags.getOrDefault("overrideStore", false)) {
      throw new BadPayloadException("RequestedService can not be null when overrideStore is TRUE");
    }

    if (orchestrationFlags.getOrDefault("onlyPreferred", false)) {
      List<PreferredProvider> tmp = new ArrayList<>();
      for (PreferredProvider provider : preferredProviders) {
        if (!provider.isValid()) {
          tmp.add(provider);
        }
      }
      preferredProviders.removeAll(tmp);
      if (preferredProviders.isEmpty()) {
        throw new BadPayloadException("There is no valid PreferredProvider, but \"onlyPreferred\" is set to true");
      }
    }

    if (orchestrationFlags.getOrDefault("enableQoS", false) && (requestedQoS.isEmpty() || commands.isEmpty())) {
      throw new BadPayloadException("RequestedQoS or commands hashmap is empty while \"enableQoS\" is set to true");
    }
  }

}
