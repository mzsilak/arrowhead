/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.gateway.thread;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.messages.ConnectToConsumerRequest;
import eu.arrowhead.core.gateway.GatewayMain;
import eu.arrowhead.core.gateway.GatewayService;
import eu.arrowhead.core.gateway.model.GatewayEncryption;
import eu.arrowhead.core.gateway.model.GatewaySession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SecureServerSocketThread extends Thread {

  private int port;
  private SSLServerSocket sslServerSocket;
  private SSLSocket sslConsumerSocket;
  private ConnectToConsumerRequest connectionRequest;
  private GatewaySession gatewaySession;
  private GatewayEncryption gatewayEncryption = new GatewayEncryption();

  private static Boolean isAesKey = true;
  private static final Logger log = LogManager.getLogger(SecureServerSocketThread.class.getName());

  public SecureServerSocketThread(GatewaySession gatewaySession, int port, ConnectToConsumerRequest connectionRequest) {
    this.port = port;
    this.connectionRequest = connectionRequest;
    this.gatewaySession = gatewaySession;
  }

  public void run() {
    log.debug("SecureServerSocket thread started");
    // Socket for server to listen at.
    SSLServerSocketFactory serverFactory = GatewayMain.serverContext.getServerSocketFactory();
    try {
      sslServerSocket = (SSLServerSocket) serverFactory.createServerSocket(port);
      sslServerSocket.setNeedClientAuth(true);
      sslServerSocket.setSoTimeout(connectionRequest.getTimeout());
      System.out.println("Secure serverSocket is now running at port: " + port + "\n");
      log.info("Create SSL ServerSocket for Consumer");
    } catch (IOException e) {
      e.printStackTrace();
      log.error("Creating secure serverSocket failed.");
    }

    try {
      // Accept a client connection once Server receives one.
      sslConsumerSocket = (SSLSocket) sslServerSocket.accept();
      Channel channel = gatewaySession.getChannel();

      InputStream inConsumer = sslConsumerSocket.getInputStream();
      OutputStream outConsumer = sslConsumerSocket.getOutputStream();

      Consumer consumer = new DefaultConsumer(channel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
          if (isAesKey) {
            try {
              isAesKey = false;
              gatewayEncryption.setEncryptedAESKey(body);
              log.info("AES Key received.");
              System.out.println("AES Key received.");
            } catch (NegativeArraySizeException e) {
              log.error("Communication failed (Error occurred or remote peer closed the socket)");
              GatewayService.consumerSideClose(gatewaySession, port, sslConsumerSocket, sslServerSocket, connectionRequest.getQueueName());
              throw new ArrowheadException(e.getMessage(), e);
            }
          } else {
            isAesKey = true;
            gatewayEncryption.setEncryptedIVAndMessage(body);
            byte[] decryptedMessage = GatewayService.decryptMessage(gatewayEncryption);
            outConsumer.write(decryptedMessage);
            log.info("Sending the response to Consumer");
            System.out.println("Broker response: ");
            System.out.println(new String(decryptedMessage));
          }
        }

      };

      Consumer controlConsumer = new DefaultConsumer(channel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
          if (new String(body).equals("close")) {
            GatewayService.consumerSideClose(gatewaySession, port, sslConsumerSocket, sslServerSocket, connectionRequest.getQueueName());
          }
        }
      };

      //noinspection InfiniteLoopStatement
      while (true) {
        // Get the request from the Consumer
        byte[] inputFromConsumer = new byte[1024];
        byte[] inputFromConsumerFinal = new byte[inConsumer.read(inputFromConsumer)];
        System.arraycopy(inputFromConsumer, 0, inputFromConsumerFinal, 0, inputFromConsumerFinal.length);
        GatewayEncryption request = GatewayService.encryptMessage(inputFromConsumerFinal, connectionRequest.getProviderGWPublicKey());
        try {
          channel.basicPublish("", connectionRequest.getQueueName(), null, request.getEncryptedAESKey());
        } catch (IOException | NegativeArraySizeException e) {
          log.error("Communication failed (Error occurred or remote peer closed the socket)");
          GatewayService.consumerSideClose(gatewaySession, port, sslConsumerSocket, sslServerSocket, connectionRequest.getQueueName());
          throw new ArrowheadException(e.getMessage(), e);
        }
        channel.basicPublish("", connectionRequest.getQueueName(), null, request.getEncryptedIVAndMessage());
        channel.basicConsume(connectionRequest.getQueueName().concat("_resp"), true, consumer);
        channel.basicConsume(connectionRequest.getControlQueueName().concat("_resp"), true, controlConsumer);
      }

    } catch (IOException | NegativeArraySizeException e) {
      log.info("Remote peer properly closed the socket.");
      GatewayService.consumerSideClose(gatewaySession, port, sslConsumerSocket, sslServerSocket, connectionRequest.getQueueName());
      e.printStackTrace();
    }
  }

}
