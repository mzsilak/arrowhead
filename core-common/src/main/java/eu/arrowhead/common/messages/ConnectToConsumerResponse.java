/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.common.messages;

public class ConnectToConsumerResponse {

  private int serverSocketPort;

  public ConnectToConsumerResponse() {
  }

  public ConnectToConsumerResponse(int serverSocketPort) {
    this.serverSocketPort = serverSocketPort;
  }

  public int getServerSocketPort() {
    return serverSocketPort;
  }

  public void setServerSocketPort(int serverSocketPort) {
    this.serverSocketPort = serverSocketPort;
  }

}
