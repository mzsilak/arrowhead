/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.deviceregistry;

import eu.arrowhead.common.ArrowheadMain;
import eu.arrowhead.common.misc.CoreSystem;

public class DeviceRegistryMain extends ArrowheadMain {

  private DeviceRegistryMain(String[] args) {

    String[] packages = {
    		"eu.arrowhead.common.exception", 
    		"eu.arrowhead.common.json", 
    		"eu.arrowhead.common.filter", 
				"eu.arrowhead.core.deviceregistry"};
    init(CoreSystem.DEVICE_REGISTRY, args, null, packages);


    listenForInput();
  }

  public static void main(String[] args) {
    new DeviceRegistryMain(args);
  }
}
