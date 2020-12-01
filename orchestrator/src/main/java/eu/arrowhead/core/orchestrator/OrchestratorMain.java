/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.orchestrator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import eu.arrowhead.common.ArrowheadMain;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.misc.CoreSystem;
import eu.arrowhead.common.misc.CoreSystemService;
import eu.arrowhead.common.misc.GetCoreSystemServicesTask;
import eu.arrowhead.common.misc.NeedsCoreSystemService;


public class OrchestratorMain extends ArrowheadMain implements NeedsCoreSystemService {

  public static TimerTask getServicesTask;

  static boolean USE_GATEKEEPER = true;
  static String SR_BASE_URI;
  private static String AUTH_CONTROL_URI;
  private static String TOKEN_GEN_URI;
  private static String GSD_SERVICE_URI;
  private static String ICN_SERVICE_URI;
  private static final String GET_CORE_SYSTEM_URLS_ERROR_MESSAGE = "The Orchestrator core system has not acquired the addresses of the "
      + "Authorization and Gatekeeper core systems yet from the Service Registry. Wait 15 seconds and retry your request";

  private OrchestratorMain(String[] args) {
    String[] packages = {"eu.arrowhead.common", "eu.arrowhead.core.orchestrator"};
    init(CoreSystem.ORCHESTRATOR, args, null, packages);
    listenForInput();
  }

  public static void main(String[] args) {
    new OrchestratorMain(args);
  }

  @Override
  protected void init(CoreSystem coreSystem, String[] args, Set<Class<?>> classes, String[] packages) {
    super.init(coreSystem, args, classes, packages);
    for (String s: args) {
        if (s.equals("-opcua")) {
			startUaServer("orchestrator");
            new OrchestratorOpcUa(getArrowheadOpcUaServer());
        }
    }
    argLoop:
    for (String arg : args) {
      switch (arg) {
        case "-nogk":
          USE_GATEKEEPER = false;
          break argLoop;
      }
    }
    SR_BASE_URI = srBaseUri;

    List<String> serviceDefs = new ArrayList<>(
        Arrays.asList(CoreSystemService.AUTH_CONTROL_SERVICE.getServiceDef(), CoreSystemService.TOKEN_GEN_SERVICE.getServiceDef()));
    if (USE_GATEKEEPER) {
      serviceDefs.addAll(Arrays.asList(CoreSystemService.GSD_SERVICE.getServiceDef(), CoreSystemService.ICN_SERVICE.getServiceDef()));
    }
    getServicesTask = new GetCoreSystemServicesTask(this, serviceDefs);
    Timer timer = new Timer();
    //TODO period to default.conf here + gatekeeper
    timer.schedule(getServicesTask, 15L * 1000L, 60L * 60L * 1000L); //15 sec delay, 1 hour period

    listenForInput();
  }

  //NOTE if a service def is changed, it needs to be modified here too!
  //TODO find a way to make the switch/case work without the hardcoded strings
  @Override
  public void getCoreSystemServiceURIs(Map<String, String[]> uriMap) {
    for (Entry<String, String[]> entry : uriMap.entrySet()) {
      switch (entry.getKey()) {
        case "AuthorizationControl":
          AUTH_CONTROL_URI = entry.getValue()[0];
          break;
        case "TokenGeneration":
          TOKEN_GEN_URI = entry.getValue()[0];
          break;
        case "GlobalServiceDiscovery":
          GSD_SERVICE_URI = entry.getValue()[0];
          break;
        case "InterCloudNegotiations":
          ICN_SERVICE_URI = entry.getValue()[0];
          break;
        default:
          break;
      }
    }
    System.out.println("Core system URLs acquired/updated.");
  }

  static String getAuthControlUri() {
    if (AUTH_CONTROL_URI == null) {
      throw new ArrowheadException(GET_CORE_SYSTEM_URLS_ERROR_MESSAGE, 500);
    }
    return AUTH_CONTROL_URI;
  }

  static String getTokenGenUri() {
    if (TOKEN_GEN_URI == null) {
      throw new ArrowheadException(GET_CORE_SYSTEM_URLS_ERROR_MESSAGE, 500);
    }
    return TOKEN_GEN_URI;
  }

  static String getGsdServiceUri() {
    if (GSD_SERVICE_URI == null) {
      throw new ArrowheadException(GET_CORE_SYSTEM_URLS_ERROR_MESSAGE, 500);
    }
    return GSD_SERVICE_URI;
  }

  static String getIcnServiceUri() {
    if (ICN_SERVICE_URI == null) {
      throw new ArrowheadException(GET_CORE_SYSTEM_URLS_ERROR_MESSAGE, 500);
    }
    return ICN_SERVICE_URI;
  }
}
