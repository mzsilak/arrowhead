/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.common.database;

import eu.arrowhead.common.json.support.ArrowheadSystemSupport;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.validator.constraints.NotBlank;

@Entity
@Table(name = "arrowhead_system", uniqueConstraints = {@UniqueConstraint(columnNames = {"system_name", "address", "port"})})
public class ArrowheadSystem {

  @Id
  @GenericGenerator(name = "table_generator", strategy = "org.hibernate.id.enhanced.TableGenerator")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "table_generator")
  private Long id;

  @NotBlank
  @Size(max = 255, message = "System name must be 255 character at max")
  @Pattern(regexp = "[\\p{L}0-9-_:]+", message =
      "System name can only contain alphanumerical characters and some special characters (dash, "
      + "underscore and colon)")
  @Column(name = "system_name")
  private String systemName;

  @NotBlank
  @Size(min = 3, max = 255, message = "Address must be between 3 and 255 characters")
  private String address;

  @NotNull
  @Min(value = 1, message = "Port can not be less than 1")
  @Max(value = 65535, message = "Port can not be greater than 65535")
  private Integer port;

  @Column(name = "authentication_info")
  @Size(max = 2047, message = "Authentication information must be 2047 character at max")
  private String authenticationInfo;

  public ArrowheadSystem() {
  }

  public ArrowheadSystem(String systemName, String address, Integer port, String authenticationInfo) {
    this.systemName = systemName;
    this.address = address;
    this.port = port;
    this.authenticationInfo = authenticationInfo;
  }

  public ArrowheadSystem(String json) {
    String[] fields = json.split(",");
    this.systemName = fields[0].equals("null") ? null : fields[0];
    this.address = fields[1].equals("null") ? null : fields[1];
    this.port = Integer.valueOf(fields[2]);

    if (fields.length == 4) {
      this.authenticationInfo = fields[3].equals("null") ? null : fields[3];
    }
  }

  public ArrowheadSystem(ArrowheadSystemSupport system) {
    this.systemName = system.getSystemGroup() + "_" + system.getSystemName();
    this.address = system.getAddress();
    this.port = system.getPort();
    this.authenticationInfo = system.getAuthenticationInfo();
  }

  @SuppressWarnings("CopyConstructorMissesField")
  public ArrowheadSystem(ArrowheadSystem system) {
    this.systemName = system.systemName;
    this.address = system.address;
    this.port = system.port;
    this.authenticationInfo = system.authenticationInfo;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getSystemName() {
    return systemName;
  }

  public void setSystemName(String systemName) {
    this.systemName = systemName;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getAuthenticationInfo() {
    return authenticationInfo;
  }

  public void setAuthenticationInfo(String authenticationInfo) {
    this.authenticationInfo = authenticationInfo;
  }

  public String toArrowheadCommonName(String operator, String cloudName) {
    if (systemName.contains(".") || operator.contains(".") || cloudName.contains(".")) {
      throw new IllegalArgumentException("The string fields can not contain dots!");
    }
    //throws NPE if any of the fields are null
    return systemName.concat(".").concat(cloudName).concat(".").concat(operator).concat(".").concat("arrowhead.eu");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ArrowheadSystem)) {
      return false;
    }
    ArrowheadSystem that = (ArrowheadSystem) o;
    return Objects.equals(systemName, that.systemName) && Objects.equals(address, that.address) && Objects.equals(port, that.port);
  }

  @Override
  public int hashCode() {
    return Objects.hash(systemName, address, port);
  }

  //NOTE ArrowheadSystemKeyDeserializer relies on this implementation, do not change it without changing the (String json) constructor
  @Override
  public String toString() {
    return systemName + "," + address + "," + port + "," + authenticationInfo;
  }

  public void partialUpdate(ArrowheadSystem other) {
    this.systemName = other.getSystemName() != null ? other.getSystemName() : this.systemName;
    this.address = other.getAddress() != null ? other.getAddress() : this.address;
    this.port = other.getPort() != null ? other.getPort() : this.port;
    this.authenticationInfo = other.getAuthenticationInfo() != null ? other.getAuthenticationInfo() : this.authenticationInfo;
  }

}
