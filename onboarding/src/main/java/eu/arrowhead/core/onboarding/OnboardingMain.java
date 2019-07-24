/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.onboarding;

import com.google.common.collect.Sets;
import eu.arrowhead.common.ArrowheadMain;
import eu.arrowhead.common.Utility;
import eu.arrowhead.common.database.ArrowheadService;
import eu.arrowhead.common.database.ArrowheadSystem;
import eu.arrowhead.common.database.ServiceRegistryEntry;
import eu.arrowhead.common.messages.IntraCloudAuthEntry;
import eu.arrowhead.common.messages.ServiceQueryForm;
import eu.arrowhead.common.messages.ServiceQueryResult;
import eu.arrowhead.common.misc.CoreSystem;
import eu.arrowhead.common.misc.CoreSystemService;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.UriBuilder;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class OnboardingMain extends ArrowheadMain {

  private final Logger logger = LogManager.getLogger(OnboardingMain.class);

  private OnboardingMain(String[] args) {
    String[] packages = {"eu.arrowhead.common.exception", "eu.arrowhead.common.json", "eu.arrowhead.common.filter",
      "eu.arrowhead.core.onboarding"};

    setRequestClientCertificate(false);
    init(CoreSystem.ONBOARDING, args, null, packages);

    listenForInput();
  }

  protected void systemRegistrationCallback(final ArrowheadSystem onboardingSystem) {
    final URI serviceRegistryUri = UriBuilder.fromUri(srBaseUri).build();


    final Set<String> restInterface = Sets.newHashSet("HTTP-SECURE-JSON", "HTTP-INSECURE-JSON");

    final ArrowheadService authService = getService(CoreSystemService.AUTH_CONTROL_SERVICE, restInterface);
    final ServiceRegistryEntry authEntry = lookupService(authService);
    final ArrowheadSystem authSystem = authEntry.getProvider();

    final String authBaseUri = Utility.getUri(authSystem.getAddress(), authSystem.getPort(),
                                              authEntry.getServiceURI(), isSecure, false);
    final String authMgmtUri = UriBuilder.fromUri(authBaseUri).path("mgmt/intracloud").build().toString();

    logger.info("Registering access rights on authorization system");
    lookupAndAuthorize(authMgmtUri, onboardingSystem, CoreSystemService.SERVICE_LOOKUP_SERVICE, restInterface);
    lookupAndAuthorize(authMgmtUri, onboardingSystem, CoreSystemService.ORCH_SERVICE, restInterface);
    lookupAndAuthorize(authMgmtUri, onboardingSystem, CoreSystemService.DEVICE_REGISTRY_SERVICE, restInterface);
    lookupAndAuthorize(authMgmtUri, onboardingSystem, CoreSystemService.SYSTEM_REGISTRY_SERVICE, restInterface);
    lookupAndAuthorize(authMgmtUri, onboardingSystem, CoreSystemService.SERVICE_REGISTRY_SERVICE, restInterface);
  }

  private void lookupAndAuthorize(final String authMgmtUri,
                                             final ArrowheadSystem consumer,
                                             final CoreSystemService coreSystemService,
                                             final Set<String> restInterface)
  {
    final ArrowheadService service = getService(coreSystemService, restInterface);
    final ServiceRegistryEntry serviceRegistryEntry = lookupService(service);
    final ArrowheadSystem system = serviceRegistryEntry.getProvider();
    createAuthRule(authMgmtUri, consumer, system, service);
  }


  private void createAuthRule(final String authMgmtUri,
                              final ArrowheadSystem consumer,
                              final ArrowheadSystem provider,
                              final ArrowheadService service)
  {
    final IntraCloudAuthEntry intraCloudAuthEntry = new IntraCloudAuthEntry();
    intraCloudAuthEntry.setConsumer(consumer);
    intraCloudAuthEntry.setProviderList(Arrays.asList(provider));
    intraCloudAuthEntry.setServiceList(Arrays.asList(service));
    Utility.sendRequest(authMgmtUri, "POST", intraCloudAuthEntry);
  }

  private ArrowheadService getService(final CoreSystemService service, final Set<String> interfaces) {
    final String serviceDef = Utility.createSD(service.getServiceDef(), isSecure);
    return new ArrowheadService(serviceDef, interfaces, Collections.emptyMap());
  }

  private ServiceRegistryEntry lookupService(final ArrowheadService service) {
    final URI serviceLookupUri = UriBuilder.fromUri(srBaseUri).path("query").build();
    final ServiceQueryForm serviceQueryForm = new ServiceQueryForm(service, true, false);

    final List<ServiceRegistryEntry> serviceQueryData;

    ServiceQueryResult lookupResult = null;
    Response response = null;
    boolean success = false;

    logger.info("Searching for \"" + service.getServiceDefinition() + "\" on service registry");

    while (!success) {
      try {
        response = Utility.sendRequest(serviceLookupUri.toString(), "PUT", serviceQueryForm);

        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
          lookupResult = response.readEntity(ServiceQueryResult.class);
          if (lookupResult.isValid() && lookupResult.getServiceQueryData().size() > 0) {
            success = true;
          } else {
            logger.info("Could not find any \"" + service.getServiceDefinition()
                          + "\" on service registry, retrying in 15 seconds");
            sleep(15000);
          }
        } else {
          logger.warn("Service Registry returned HTTP " + response.getStatus());
          sleep(15000);
        }

      } catch (Exception e) {
        logger.warn("Service Registry is unavailable at the moment, retrying in 15 seconds...");
        sleep(15000);
      }
    }

    serviceQueryData = lookupResult.getServiceQueryData();
    return serviceQueryData.get(0);
  }

  private void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      // noop
    }
  }


  public static void main(String[] args) {
    new OnboardingMain(args);
  }
}
