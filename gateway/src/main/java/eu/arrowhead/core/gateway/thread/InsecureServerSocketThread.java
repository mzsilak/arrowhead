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
import eu.arrowhead.core.gateway.GatewayService;
import eu.arrowhead.core.gateway.model.GatewaySession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InsecureServerSocketThread extends Thread {

  private int port;
  private ServerSocket serverSocket;
  private Socket consumerSocket;
  private ConnectToConsumerRequest connectionRequest;
  private GatewaySession gatewaySession;

  private static Boolean isFirstMessage = true;
  private static final Logger log = LogManager.getLogger(InsecureServerSocketThread.class.getName());

  public InsecureServerSocketThread(GatewaySession gatewaySession, int port, ConnectToConsumerRequest connectionRequest) {
    this.port = port;
    this.connectionRequest = connectionRequest;
    this.gatewaySession = gatewaySession;
  }

  public void run() {
    log.debug("InsecureServerSocket thread started");

    try {
      serverSocket = new ServerSocket(port);
      System.out.println("Insecure serverSocket is now running at port: " + port);
      log.info("Insecure serverSocket is now running at port: " + port);
    } catch (IOException e) {
      log.error("Creating insecure ServerSocket failed");
      throw new ArrowheadException(e.getMessage(), e);
    }

    try {
      // Create socket for Consumer
      serverSocket.setSoTimeout(connectionRequest.getTimeout());
      consumerSocket = serverSocket.accept();
      consumerSocket.setSoTimeout(connectionRequest.getTimeout());

      InputStream inConsumer = consumerSocket.getInputStream();
      OutputStream outConsumer = consumerSocket.getOutputStream();
      log.info("Create socket for Consumer");
      Channel channel = gatewaySession.getChannel();

      Consumer consumer = new DefaultConsumer(channel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
          outConsumer.write(body);
          System.out.println("Broker response: ");
          System.out.println(new String(body));
        }

      };

      Consumer controlConsumer = new DefaultConsumer(channel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
          if (new String(body).equals("close")) {
            GatewayService.consumerSideClose(gatewaySession, port, consumerSocket, serverSocket, connectionRequest.getQueueName());
          }
        }
      };

      //noinspection InfiniteLoopStatement
      while (true) {
        // Get the request from the Consumer
        byte[] inputFromConsumer = new byte[1024];
        byte[] inputFromConsumerFinal = new byte[inConsumer.read(inputFromConsumer)];

        System.arraycopy(inputFromConsumer, 0, inputFromConsumerFinal, 0, inputFromConsumerFinal.length);

        System.out.println("Consumer's final request:");
        System.out.println(new String(inputFromConsumerFinal));

        channel.basicPublish("", connectionRequest.getQueueName(), null, inputFromConsumerFinal);
        log.info("Publishing the request to the queue");

        channel.basicConsume(connectionRequest.getQueueName().concat("_resp"), true, consumer);
        channel.basicConsume(connectionRequest.getControlQueueName().concat("_resp"), true, controlConsumer);
        isFirstMessage = false;
      }

    } catch (IOException | NegativeArraySizeException e) {
      GatewayService.consumerSideClose(gatewaySession, port, consumerSocket, serverSocket, connectionRequest.getQueueName());
      if (isFirstMessage) {
        log.error("Communication failed (Error occurred or remote peer closed the socket)");
        throw new ArrowheadException(e.getMessage(), e);
      }
      System.out.println(connectionRequest.getQueueName() + " was closed by the other side!");
      e.printStackTrace();
    }
  }

}
