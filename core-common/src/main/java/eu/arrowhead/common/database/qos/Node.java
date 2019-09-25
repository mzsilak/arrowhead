/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.common.database.qos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlTransient;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "node", uniqueConstraints = {@UniqueConstraint(columnNames = {"device_model_code"})})
public class Node {

  @Column(name = "id")
  @Id
  @GenericGenerator(name = "table_generator", strategy = "org.hibernate.id.enhanced.TableGenerator")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "table_generator")
  private int id;

  @Column(name = "device_model_code")
  private String deviceModelCode;

  @JoinColumn(name = "deployed_system_id")
  @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
  private List<DeployedSystem> deployedSystems = new ArrayList<>();

  @ElementCollection(fetch = FetchType.LAZY)
  @MapKeyColumn(name = "capability_key")
  @Column(name = "capability_value")
  @CollectionTable(name = "node_processing_capabilities", joinColumns = @JoinColumn(name = "id"))
  private Map<String, String> processingCapabilities = new HashMap<>();

  public Node() {
  }

  public Node(String deviceModelCode, List<DeployedSystem> deployedSystems, Map<String, String> processingCapabilities) {
    this.deviceModelCode = deviceModelCode;
    this.deployedSystems = deployedSystems;
    this.processingCapabilities = processingCapabilities;
  }

  @XmlTransient
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getDeviceModelCode() {
    return deviceModelCode;
  }

  public void setDeviceModelCode(String deviceModelCode) {
    this.deviceModelCode = deviceModelCode;
  }

  public List<DeployedSystem> getDeployedSystems() {
    return deployedSystems;
  }

  public void setDeployedSystems(List<DeployedSystem> deployedSystems) {
    this.deployedSystems = deployedSystems;
  }

  public Map<String, String> getProcessingCapabilities() {
    return processingCapabilities;
  }

  public void setProcessingCapabilities(Map<String, String> processingCapabilities) {
    this.processingCapabilities = processingCapabilities;
  }

}
