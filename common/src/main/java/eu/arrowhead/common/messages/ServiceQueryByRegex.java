package eu.arrowhead.common.messages;

public class ServiceQueryByRegex {

  private String regularExpression;

  public ServiceQueryByRegex() {
  }

  public ServiceQueryByRegex(String regularExpression) {
    this.regularExpression = regularExpression;
  }

  public String getRegularExpression() {
    return regularExpression;
  }

  public void setRegularExpression(String regularExpression) {
    this.regularExpression = regularExpression;
  }
}
