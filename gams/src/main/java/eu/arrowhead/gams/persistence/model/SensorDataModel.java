package eu.arrowhead.gams.persistence.model;

import eu.arrowhead.gams.api.model.SensorDataState;
import java.time.LocalDateTime;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

@Entity
@Table(name = "sensor_data")
public class SensorDataModel extends AbstractGamsModel implements Comparable<SensorDataModel> {

    @Column(name = "sensor_id", nullable = true, updatable = false)
    private String sensorId;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "value", nullable = false, updatable = false)
    private Long value;

    @Column(name = "magnitude", nullable = false, columnDefinition = "bigint not null default 1", updatable = false)
    private Long magnitude;

    @Column(name = "unit", nullable = true, updatable = false)
    private String unit;

    @Column(name = "state", nullable = false, updatable = true)
    @Enumerated(EnumType.STRING)
    private SensorDataState state;

    public SensorDataModel() {
        super();
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    public Long getMagnitude() {
        return magnitude;
    }

    public void setMagnitude(Long magnitude) {
        this.magnitude = magnitude;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public SensorDataState getState() {
        return state;
    }

    public void setState(SensorDataState state) {
        this.state = state;
    }

    @Override
    protected void appendToString(StringBuilder sb) {
        sb.append(", sensorId=").append(sensorId);
        sb.append(", timestamp=").append(timestamp);
        sb.append(", value=").append(value);
        sb.append(", magnitude=").append(magnitude);
        sb.append(", unit=").append(unit);
        sb.append(", state=").append(state);
    }

    @Override
    public int compareTo(SensorDataModel other) {
        Objects.requireNonNull(other);
        return this.timestamp.compareTo(other.timestamp);
    }
}
