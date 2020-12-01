/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.qos;

import eu.arrowhead.common.DatabaseManager;
import eu.arrowhead.common.Utility;
import eu.arrowhead.common.database.ArrowheadSystem;
import eu.arrowhead.common.database.qos.DeployedSystem;
import eu.arrowhead.common.database.qos.MessageStream;
import eu.arrowhead.common.database.qos.Network;
import eu.arrowhead.common.database.qos.NetworkDevice;
import eu.arrowhead.common.database.qos.ResourceReservation;
import eu.arrowhead.common.exception.DriverNotFoundException;
import eu.arrowhead.common.exception.ReservationException;
import eu.arrowhead.common.messages.MonitorRule;
import eu.arrowhead.common.messages.QoSReservationCommand;
import eu.arrowhead.common.messages.QoSReservationResponse;
import eu.arrowhead.common.messages.QoSReserve;
import eu.arrowhead.common.messages.QoSVerificationResponse;
import eu.arrowhead.common.messages.QoSVerifierResponse;
import eu.arrowhead.common.messages.QoSVerify;
import eu.arrowhead.core.qos.algorithms.VerifierAlgorithmFactory;
import eu.arrowhead.core.qos.drivers.DriversFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class QoSManagerService {

  private static final Logger log = LogManager.getLogger(QoSManagerService.class.getName());
  private static final VerifierAlgorithmFactory algorithmFactory = VerifierAlgorithmFactory.getInstance();
  private static final DriversFactory driverFactory = DriversFactory.getInstance();
  private static final DatabaseManager dm = DatabaseManager.getInstance();
  private static final HashMap<String, Object> restrictionMap = new HashMap<>();

  private QoSManagerService() throws AssertionError {
    throw new AssertionError("QoSManagerService is a non-instantiable class");
  }

  /**
   * Verifies if the requestedQoS is possible on the selected providers.
   *
   * @param message QoSVerify parameters.
   *
   * @return Returns if is possible or not and why.
   */

  static QoSVerificationResponse qosVerify(QoSVerify message) {
    log.info("QoS: Verifying QoS paramteres.");

    NetworkDevice consumerNetworkDevice = getNetworkDeviceFromSystem(message.getConsumer());
    List<ResourceReservation> consumerReservations = getReservationsFromSystem(message.getConsumer());

    QoSVerificationResponse qosVerificationResponse = new QoSVerificationResponse();
    for (ArrowheadSystem provider : message.getProviders()) {
      NetworkDevice providerNetworkDevice = getNetworkDeviceFromSystem(provider);
      if (providerNetworkDevice == null) {
        continue;
      }
      List<ResourceReservation> providerReservations = getReservationsFromSystem(provider);

      Network network = providerNetworkDevice.getNetwork();
      if (network == null) {
        continue;
      }

      try {
        QoSVerifierResponse response = algorithmFactory
            .verify(network.getNetworkType(), providerNetworkDevice.getNetworkCapabilities(), consumerNetworkDevice.getNetworkCapabilities(),
                    providerReservations, consumerReservations, message.getRequestedQoS(), message.getCommands());

        qosVerificationResponse.addResponse(provider, response.getResponse());
        if (!response.getResponse()) {
          qosVerificationResponse.addRejectMotivation(provider, response.getRejectMotivation());
        }
      } catch (Exception ex) {
        log.error(ex.getClass() + " in QosManagerService:qosVerify");
        ex.printStackTrace();
      }
    }

    log.info("QoS: QoS paramteres verified.");
    return qosVerificationResponse;
  }
  // request in a blocking for loop

  private static NetworkDevice getNetworkDeviceFromSystem(ArrowheadSystem system) {
    restrictionMap.clear();
    restrictionMap.put("systemName", system.getSystemName());
    ArrowheadSystem retrievedSystem = dm.get(ArrowheadSystem.class, restrictionMap);

    restrictionMap.clear();
    restrictionMap.put("system", retrievedSystem);
    DeployedSystem deployedSystem = dm.get(DeployedSystem.class, restrictionMap);

    return deployedSystem.getNetworkDevice();
  }


  private static List<ResourceReservation> getReservationsFromSystem(ArrowheadSystem system) {
    restrictionMap.clear();
    restrictionMap.put("systemName", system.getSystemName());
    ArrowheadSystem retrievedSystem = dm.get(ArrowheadSystem.class, restrictionMap);

    //limiting the filtering to just consumers/providers might be desired (need an extra boolean for argument for that)
    restrictionMap.clear();
    restrictionMap.put("consumer", retrievedSystem);
    restrictionMap.put("provider", retrievedSystem);
    List<MessageStream> messageStreams = dm.getAllOfEither(MessageStream.class, restrictionMap);
    List<ResourceReservation> reservations = new ArrayList<>();
    for (MessageStream ms : messageStreams) {
      reservations.add(ms.getQualityOfService());
    }

    return reservations;
  }

  /**
   * Reserves a QoS on the consumer and provider stream.
   *
   * @param message QoSReservation parameters.
   *
   * @return Returns if the reservation was possible.
   *
   * @throws ReservationException The reservation on the devices was not possible.
   * @throws DriverNotFoundException The network type doesnt have a driver assigned.
   */
  static QoSReservationResponse qosReserve(QoSReserve message) throws ReservationException, DriverNotFoundException {
    ArrowheadSystem consumer = message.getConsumer();
    ArrowheadSystem provider = message.getProvider();

    NetworkDevice consumerNetworkDevice = getNetworkDeviceFromSystem(consumer);
    NetworkDevice providerNetworkDevice = getNetworkDeviceFromSystem(provider);
    if (providerNetworkDevice == null) {
      throw new ReservationException("");
    }

    Network network = providerNetworkDevice.getNetwork();
    if (network == null) {
      return new QoSReservationResponse(false);
    }

    //Generate commands
    Map<String, String> commands = new HashMap<>();
    try {
      commands = driverFactory
          .generateCommands(network.getNetworkType(), network.getNetworkConfigurations(), provider, consumer, message.getService(),
                            message.getCommands(), message.getRequestedQoS());
    } catch (Exception e) {
      e.printStackTrace();
    }

    ResourceReservation reservation = new ResourceReservation("UP", message.getRequestedQoS());
    MessageStream stream = new MessageStream(message.getService(), consumer, provider, reservation, commands, network.getNetworkType());
    dm.save(stream);

    //Send MonitorRule to QoSMonitor
    Map<String, String> parameters = message.getRequestedQoS();
    parameters.putAll(network.getNetworkConfigurations());
    MonitorRule rule = new MonitorRule(network.getNetworkType().toUpperCase(), provider, consumer, parameters, false);
    Response response = Utility.sendRequest(QoSMain.MONITOR_URL, "POST", rule);
    boolean ruleApplied = response.getStatusInfo().getFamily() == Family.SUCCESSFUL;

    return new QoSReservationResponse(ruleApplied,
                                      new QoSReservationCommand(message.getService(), message.getConsumer(), message.getProvider(), commands,
                                                                message.getRequestedQoS()));
  }

}
