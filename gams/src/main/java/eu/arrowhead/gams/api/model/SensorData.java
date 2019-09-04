package eu.arrowhead.gams.api.model;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.StringJoiner;

public class SensorData {

    private String sensorId;
    private ZonedDateTime timestamp;
    private Long value;
    private Long magnitude;
    private String unit;
    private SensorDataState state;

    public SensorData() {
        super();
    }

    public SensorData(String sensorId, ZonedDateTime timestamp, Long value, Long magnitude, String unit,
                      SensorDataState state) {
        this.sensorId = sensorId;
        this.timestamp = timestamp;
        this.value = value;
        this.magnitude = magnitude;
        this.unit = unit;
        this.state = state;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SensorData that = (SensorData) o;
        return Objects.equals(sensorId, that.sensorId) && timestamp.equals(that.timestamp) && value.equals(that.value)
            && magnitude.equals(that.magnitude) && Objects.equals(unit, that.unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sensorId, timestamp, value, magnitude, unit);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SensorData.class.getSimpleName() + "[", "]").add("sensorId='" + sensorId + "'")
                                                                                  .add("timestamp=" + timestamp)
                                                                                  .add("value=" + value)
                                                                                  .add("magnitude=" + magnitude)
                                                                                  .add("unit='" + unit + "'")
                                                                                  .toString();
    }
}
