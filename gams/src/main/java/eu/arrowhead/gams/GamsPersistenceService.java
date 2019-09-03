package eu.arrowhead.gams;

import eu.arrowhead.gams.api.model.SensorData;
import eu.arrowhead.gams.errors.InstanceNotFoundException;
import eu.arrowhead.gams.persistence.model.SensorDataModel;
import java.util.Set;
import java.util.UUID;

public interface GamsPersistenceService {

    SensorDataModel persistSensorData(SensorDataModel sensorData) throws InstanceNotFoundException;

    SensorDataModel mapSensorData(UUID uuid, SensorData data);

    Set<SensorDataModel> querySensorData(UUID uuid, Integer size) throws InstanceNotFoundException;
}
