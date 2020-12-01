/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.certificate_authority;

import eu.arrowhead.common.Utility;
import eu.arrowhead.common.exception.ArrowheadException;
import java.util.Optional;
import java.util.TimerTask;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GetAuthPublicKeyTask extends TimerTask {

  private static final Logger log = LogManager.getLogger(GetAuthPublicKeyTask.class);

  @Override
  public void run() {
    try {
      Optional<String[]> optionalUri = Utility.getServiceInfo("AuthorizationControl");
      if (optionalUri.isPresent()) {
        String authUri = optionalUri.get()[0];
        authUri = authUri.substring(0, authUri.lastIndexOf("/")) + "/authorization/mgmt/publickey";
        Response response = Utility.sendRequest(authUri, "GET", null);
        CAMain.encodedAuthPublicKey = response.readEntity(String.class);
        log.info("Authorization public key acquired, canceling the Timer calling this Task.");
        CAMain.authTimer.cancel();
      } else {
        log.info("Authorization public key could not be acquired because Authorization is offline!");
      }
    } catch (ArrowheadException e) {
      log.info("Authorization public key could not be acquired: " + e.getMessage(), e);
    }
  }
}
