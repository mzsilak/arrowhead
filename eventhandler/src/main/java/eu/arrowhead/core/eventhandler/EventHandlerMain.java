/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.eventhandler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import eu.arrowhead.common.ArrowheadMain;
import eu.arrowhead.common.misc.CoreSystem;
import eu.arrowhead.common.web.ArrowheadSystemApi;

public class EventHandlerMain extends ArrowheadMain {

  static int EVENT_PUBLISHING_TOLERANCE;

  {
    EVENT_PUBLISHING_TOLERANCE = props.getIntProperty("event_publishing_tolerance", 60);
  }

  private EventHandlerMain(String[] args) {
    Set<Class<?>> classes = new HashSet<>(
        Arrays.asList(ArrowheadSystemApi.class, EventHandlerResource.class, EventHandlerApi.class));
    String[] packages = {"eu.arrowhead.common.exception", "eu.arrowhead.common.json", "eu.arrowhead.common.filter"};
    init(CoreSystem.EVENT_HANDLER, args, classes, packages);

    //if removing old filters (based on endDate field) is requested, start the TimerTask that provides it
    if (props.getBooleanProperty("remove_old_filters", false)) {
      TimerTask pingTask = new DeleteExpiredFiltersTask();
      Timer pingTimer = new Timer();
      int interval = props.getIntProperty("check_interval", 60);
      pingTimer.schedule(pingTask, 60L * 1000L, (interval * 60L * 1000L));
    }

    for (String s: args) {
        if (s.equals("-opcua")) {
			startUaServer("eventhandler");
            new EventHandlerOpcUa(getArrowheadOpcUaServer());
        }
    }
    
    listenForInput();
  }

  public static void main(String[] args) {
    new EventHandlerMain(args);
  }

}
