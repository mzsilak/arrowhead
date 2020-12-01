/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.serviceregistry;


import com.github.danieln.dnssdjava.DnsSDBrowser;
import com.github.danieln.dnssdjava.DnsSDDomainEnumerator;
import com.github.danieln.dnssdjava.DnsSDException;
import com.github.danieln.dnssdjava.DnsSDFactory;
import com.github.danieln.dnssdjava.DnsSDRegistrator;
import com.github.danieln.dnssdjava.ServiceData;
import com.github.danieln.dnssdjava.ServiceName;
import com.github.danieln.dnssdjava.ServiceType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.TimerTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PingProvidersTask extends TimerTask {

  private static final Logger log = LogManager.getLogger(ServiceRegistry.class.getName());

  @Override
  public void run() {
    log.debug("Cleaning up DNS at " + LocalDateTime.now());
    pingAndRemoveServices();
  }

  private void pingAndRemoveServices() {
    DnsSDDomainEnumerator de = DnsSDFactory.getInstance().createDomainEnumerator(ServiceRegistryMain.DNS_DOMAIN);
    DnsSDBrowser browser = DnsSDFactory.getInstance().createBrowser(de.getBrowsingDomains());

    Collection<ServiceType> types = browser.getServiceTypes();

    if (types != null) {
      //for every type,
      for (ServiceType type : types) {
        Collection<ServiceName> instances = browser.getServiceInstances(type);

        //per every instance we shall ping
        for (ServiceName instance : instances) {
          ServiceData serviceInstanceData = browser.getServiceData(instance);
          String hostName = serviceInstanceData.getHost();
          int port = serviceInstanceData.getPort();
          RegistryUtils.removeLastChar(hostName, '.');
          boolean toBeRemoved = false;
          if (hostName.equals("0.0.0.0")) {
            toBeRemoved = true;
          } else if (!RegistryUtils.pingHost(hostName, port, ServiceRegistryMain.PING_TIMEOUT)) {
            toBeRemoved = true;
          }

          if (toBeRemoved) {
            try {
              DnsSDRegistrator registrator = RegistryUtils.createRegistrator();
              registrator.unregisterService(instance);
            } catch (DnsSDException e) {
              log.error("DNS error occured in deleting an entry." + e.getMessage());
            }
          }
        }
      }
    }
  }
}