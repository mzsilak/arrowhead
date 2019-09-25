/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.serviceregistry_sql;

import eu.arrowhead.common.database.ArrowheadService;
import eu.arrowhead.common.database.ServiceRegistryEntry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class RegistryUtils {

  static boolean pingHost(String host, int port, int timeout) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), timeout);
      return true;
    } catch (IOException e) {
      return false; // Either timeout or unreachable or failed DNS lookup.
    }
  }

  static void filterOnVersion(List<ServiceRegistryEntry> fetchedList, int targetVersion) {
    fetchedList.removeIf(current -> current.getVersion() != targetVersion);
  }

  static void filterOnVersion(List<ServiceRegistryEntry> fetchedList, int minVersion, int maxVersion) {
    fetchedList.removeIf(current -> current.getVersion() < minVersion || current.getVersion() > maxVersion);
  }

  static void filterOnMeta(List<ServiceRegistryEntry> fetchedList, Map<String, String> metadata) {
    fetchedList.removeIf(current -> !metadata.equals(current.getProvidedService().getServiceMetadata()));
  }

  static void filterOnPing(List<ServiceRegistryEntry> fetchedList) {
    fetchedList.removeIf(current -> !pingHost(current.getProvider().getAddress(), current.getProvider().getPort(),
                                              ServiceRegistryMain.PING_TIMEOUT));
  }

  static void filterOnInterfaces(List<ServiceRegistryEntry> fetchedList, ArrowheadService givenService) {
    List<ServiceRegistryEntry> toBeRemoved = new ArrayList<>();
    for (ServiceRegistryEntry entry : fetchedList) {
      if (Collections.disjoint(entry.getProvidedService().getInterfaces(), givenService.getInterfaces())) {
        toBeRemoved.add(entry);
      }
    }
    fetchedList.removeAll(toBeRemoved);
  }

}
