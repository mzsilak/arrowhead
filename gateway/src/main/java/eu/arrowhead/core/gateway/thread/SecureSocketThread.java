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
import eu.arrowhead.common.messages.ConnectToProviderRequest;
import eu.arrowhead.core.gateway.GatewayMain;
import eu.arrowhead.core.gateway.GatewayService;
import eu.arrowhead.core.gateway.model.GatewayEncryption;
import eu.arrowhead.core.gateway.model.GatewaySession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SecureSocketThread extends Thread {

  private GatewaySession gatewaySession;
  private String queueName;
  private String controlQueueName;
  private SSLSocket sslProviderSocket;
  private ConnectToProviderRequest connectionRequest;
  private GatewayEncryption gatewayEncryption = new GatewayEncryption();

  private static Boolean isAesKey = true;
  private static final Logger log = LogManager.getLogger(SecureSocketThread.class.getName());

  public SecureSocketThread(GatewaySession gatewaySession, String queueName, String controlQueueName, ConnectToProviderRequest connectionRequest) {
    this.gatewaySession = gatewaySession;
    this.queueName = queueName;
    this.controlQueueName = controlQueueName;
    this.connectionRequest = connectionRequest;
  }

  public void run() {
    try {
      log.debug("SecureSocket thread started");
      // Creating SSLsocket for Provider
      Channel channel = gatewaySession.getChannel();
      SSLSocketFactory clientFactory = GatewayMain.serverContext.getSocketFactory();
      sslProviderSocket = (SSLSocket) clientFactory
          .createSocket(connectionRequest.getProvider().getAddress(), connectionRequest.getProvider().getPort());
      sslProviderSocket.setSoTimeout(connectionRequest.getTimeout());
      InputStream inProvider = sslProviderSocket.getInputStream();
      OutputStream outProvider = sslProviderSocket.getOutputStream();
      log.info("Created SSL Socket for Provider");

      // Receiving messages through AMQP Broker
      Consumer consumer = new DefaultConsumer(channel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
          if (isAesKey) {
            isAesKey = false;
            gatewayEncryption.setEncryptedAESKey(body);
            System.out.println("AES Key received.");
          } else {
            isAesKey = true;
            gatewayEncryption.setEncryptedIVAndMessage(body);
            byte[] decryptedMessage = GatewayService.decryptMessage(gatewayEncryption);
            outProvider.write(decryptedMessage);
            log.info("Sending the encrypted request to Provider");

          }
        }
      };

      Consumer controlConsumer = new DefaultConsumer(channel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
          if (new String(body).equals("close")) {
            GatewayService.providerSideClose(gatewaySession, sslProviderSocket, queueName);
          }
        }
      };

      //noinspection InfiniteLoopStatement
      while (true) {
        channel.basicConsume(queueName, true, consumer);
        channel.basicConsume(controlQueueName, true, controlConsumer);

        // Get the answer from Provider
        byte[] inputFromProvider = new byte[1024];
        byte[] inputFromProviderFinal = new byte[inProvider.read(inputFromProvider)];
        System.arraycopy(inputFromProvider, 0, inputFromProviderFinal, 0, inputFromProviderFinal.length);
        GatewayEncryption response = GatewayService.encryptMessage(inputFromProviderFinal, connectionRequest.getConsumerGWPublicKey());

        log.info("Sending the encrypted response to Consumer");
        channel.basicPublish("", queueName.concat("_resp"), null, response.getEncryptedAESKey());
        channel.basicPublish("", queueName.concat("_resp"), null, response.getEncryptedIVAndMessage());
      }

    } catch (IOException | NegativeArraySizeException e) {
      log.info("Remote peer properly closed the socket.");
      GatewayService.providerSideClose(gatewaySession, sslProviderSocket, queueName);
      e.printStackTrace();
    }
  }

}
