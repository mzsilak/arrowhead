/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.gateway;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.misc.SecurityUtils;
import eu.arrowhead.core.gateway.model.ActiveSession;
import eu.arrowhead.core.gateway.model.GatewayEncryption;
import eu.arrowhead.core.gateway.model.GatewaySession;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.Response.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Contains miscellaneous helper functions for the Gateway.
 */

public class GatewayService {

  static ConcurrentHashMap<String, ActiveSession> activeSessions = new ConcurrentHashMap<>();

  private static final int ivSize = 16;
  private static final int keySize = 16;
  private static final ConcurrentHashMap<Integer, Boolean> portAllocationMap = GatewayService
      .initPortAllocationMap(new ConcurrentHashMap<>(), GatewayMain.minPort, GatewayMain.maxPort);
  private static final Logger log = LogManager.getLogger(GatewayService.class.getName());

  private GatewayService() throws AssertionError {
    throw new AssertionError("GatewayService is a non-instantiable class");
  }

  /**
   * Creates a channel to the Broker
   *
   * @param brokerHost The hostname of the AMQP broker to use for connections
   * @param brokerPort The port of the AMQP broker to use for connections
   * @param queueName The name of the queue, should be unique
   * @param controlQueueName The name of the queue for control messages, should be unique
   * @param isSecure The type of the channel (secure or insecure)
   *
   * @return GatewaySession, which contains a connection and a channel object
   */
  static GatewaySession createChannel(String brokerHost, int brokerPort, String queueName, String controlQueueName, boolean isSecure) {
    GatewaySession gatewaySession = new GatewaySession();
    try {
      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost(brokerHost);
      factory.setPort(brokerPort);

      if (isSecure) {
        try {
          factory.useSslProtocol(GatewayMain.clientContext);
        } catch (RuntimeException e) {
          throw new ArrowheadException("Gateway is in insecure mode, and can not create a secure channel with the AMQP broker!", e);
        }
      }

      Connection connection = factory.newConnection();
      Channel channel = connection.createChannel();
      channel.queueDeclare(queueName, false, false, false, null);
      channel.queueDeclare(queueName.concat("_resp"), false, false, false, null);
      channel.queueDeclare(controlQueueName, false, false, false, null);
      channel.queueDeclare(controlQueueName.concat("_resp"), false, false, false, null);
      gatewaySession.setConnection(connection);
      gatewaySession.setChannel(channel);
    } catch (IOException | NullPointerException | TimeoutException e) {
      log.error("Creating the channel to the Broker failed");
      throw new ArrowheadException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
    }

    log.info("Created a channel for broker: " + brokerHost + ":" + brokerPort);
    return gatewaySession;
  }

  //NOTE exception handling too general right now for payload encyption/decryption
  public static GatewayEncryption encryptMessage(byte[] message, String publicKeyString) {
    Cipher cipherRSA;
    Cipher cipherAES;
    PublicKey publicKey;
    byte[] encryptedAESKey;
    byte[] encryptedIVAndMessage;

    try {
      publicKey = SecurityUtils.getPublicKey(publicKeyString, false);
      cipherRSA = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      cipherRSA.init(Cipher.ENCRYPT_MODE, publicKey);
    } catch (GeneralSecurityException e) {
      log.fatal("The initialization of the RSA cipher failed.");
      throw new ArrowheadException(e.getMessage(), Status.INTERNAL_SERVER_ERROR.getStatusCode(), e);
    }

    // Creating the random IV (Initialization vector)
    SecureRandom random = new SecureRandom();
    byte[] IV = new byte[ivSize];
    random.nextBytes(IV);
    IvParameterSpec ivParameterSpec = new IvParameterSpec(IV);

    // Creating the random AES (Advanced Encryption Standard) key
    byte[] SECRET_KEY = new byte[keySize];
    random.nextBytes(SECRET_KEY);
    SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY, "AES");

    try {
      encryptedAESKey = cipherRSA.doFinal(secretKeySpec.getEncoded());
      cipherAES = Cipher.getInstance("AES/CBC/PKCS5PADDING");
      cipherAES.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
      byte[] encryptedMessage = cipherAES.doFinal(message);

      // Combine IV and encrypted part.
      encryptedIVAndMessage = new byte[ivSize + encryptedMessage.length];
      System.arraycopy(IV, 0, encryptedIVAndMessage, 0, ivSize);
      System.arraycopy(encryptedMessage, 0, encryptedIVAndMessage, ivSize, encryptedMessage.length);

      // Initialize and return the value
      return new GatewayEncryption(encryptedAESKey, encryptedIVAndMessage);

    } catch (GeneralSecurityException e) {
      log.fatal("Something goes wrong while AES encryption.");
      throw new ArrowheadException(e.getMessage(), Status.INTERNAL_SERVER_ERROR.getStatusCode(), e);
    }
  }

  public static byte[] decryptMessage(GatewayEncryption gatewayEncryption) {
    Cipher cipherRSA;
    Cipher cipherAES;
    byte[] decryptedMessage;

    try {
      cipherRSA = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      cipherRSA.init(Cipher.DECRYPT_MODE, GatewayMain.privateKey);
    } catch (GeneralSecurityException e) {
      log.fatal("The initialization of the RSA cipher failed.");
      throw new ArrowheadException(e.getMessage(), Status.INTERNAL_SERVER_ERROR.getStatusCode(), e);
    }

    try {
      byte[] decryptedAESKey = cipherRSA.doFinal(gatewayEncryption.getEncryptedAESKey());

      // Extract IV.
      byte[] IV_DECRYPT = new byte[ivSize];
      System.arraycopy(gatewayEncryption.getEncryptedIVAndMessage(), 0, IV_DECRYPT, 0, IV_DECRYPT.length);
      IvParameterSpec ivParameterSpec = new IvParameterSpec(IV_DECRYPT);

      // Extract encrypted part.
      int encryptedSize = gatewayEncryption.getEncryptedIVAndMessage().length - ivSize;
      byte[] encryptedBytes = new byte[encryptedSize];
      System.arraycopy(gatewayEncryption.getEncryptedIVAndMessage(), ivSize, encryptedBytes, 0, encryptedSize);

      SecretKeySpec secretKeySpec = new SecretKeySpec(decryptedAESKey, "AES");
      cipherAES = Cipher.getInstance("AES/CBC/PKCS5PADDING");
      cipherAES.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
      decryptedMessage = cipherAES.doFinal(encryptedBytes);
    } catch (GeneralSecurityException e) {
      log.fatal("Something goes wrong while AES decryption.");
      throw new ArrowheadException(e.getMessage(), Status.INTERNAL_SERVER_ERROR.getStatusCode(), e);
    }

    return decryptedMessage;
  }

  /**
   * Fill the ConcurrentHashMap with initial keys and values
   *
   * @param map ConcurrentHashMap which contains the port number and the availability
   * @param portMin The lowest port number from the allowed range
   * @param portMax The highest port number from the allowed range
   *
   * @return The initialized ConcurrentHashMap
   */
  // Integer: port; Boolean: free (true) or reserved(false)
  private static ConcurrentHashMap<Integer, Boolean> initPortAllocationMap(ConcurrentHashMap<Integer, Boolean> map, int portMin, int portMax) {
    for (int i = portMin; i <= portMax; i++) {
      map.put(i, true);
    }
    return map;
  }

  /**
   * Search for an available port in the port range
   *
   * @return serverSocketPort or null if no available port found
   */
  static Integer getAvailablePort() {
    Integer serverSocketPort;
    // Check the port range for
    ArrayList<Integer> freePorts = new ArrayList<>();
    for (Entry<Integer, Boolean> entry : portAllocationMap.entrySet()) {
      if (entry.getValue().equals(true)) {
        freePorts.add(entry.getKey());
      }
    }

    if (freePorts.isEmpty()) {
      log.error("No available port found in port range");
      throw new ArrowheadException("No available port found in port range", 500, "GatewayService:getAvailablePort");
    } else {
      serverSocketPort = freePorts.get(0);
      portAllocationMap.put(serverSocketPort, false);
    }
    return serverSocketPort;
  }

  public static void consumerSideClose(GatewaySession gatewaySession, Integer port, Socket consumerSocket, ServerSocket serverSocket,
                                       String queueName) {
    // Setting serverSocket free
    portAllocationMap.put(port, true);
    activeSessions.remove(queueName);
    try {

      gatewaySession.getChannel().close();
      gatewaySession.getConnection().close();
      if (consumerSocket != null) {
        consumerSocket.close();
      }
      serverSocket.close();
      log.info("ConsumerSocket closed");
    } catch (AlreadyClosedException | IOException | TimeoutException e) {
      log.error("Channel already closed");
    }
  }

  public static void providerSideClose(GatewaySession gatewaySession, Socket providerSocket, String queueName) {
    try {
      activeSessions.remove(queueName);
      if (providerSocket != null) {
        providerSocket.close();
      }
      gatewaySession.getChannel().close();
      gatewaySession.getConnection().close();
      log.info("ProviderSocket closed");
    } catch (AlreadyClosedException | IOException | TimeoutException e) {
      log.error("Channel already closed");
    }
  }

}
