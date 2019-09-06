package eu.arrowhead.gams;

import eu.arrowhead.gams.api.model.SensorData;
import eu.arrowhead.gams.errors.InstanceNotFoundException;
import eu.arrowhead.gams.persistence.model.GamsInstanceModel;
import eu.arrowhead.gams.persistence.model.SensorDataModel;
import java.util.Set;
import java.util.UUID;

public interface GamsPersistenceService {

    SensorDataModel persistSensorData(SensorDataModel sensorData) throws InstanceNotFoundException;

    @Deprecated
    SensorDataModel mapSensorData(UUID uuid, SensorData data);

    Set<SensorDataModel> querySensorData(UUID uuid, Integer size) throws InstanceNotFoundException;

    Iterable<GamsInstanceModel> searchGamsInstances(String namePart);

    Iterable<GamsInstanceModel> getGamsInstances();

    GamsInstanceModel persistGamsInstance(GamsInstanceModel model);

    void deleteGamsInstance(UUID uuid) throws InstanceNotFoundException;

    GamsInstanceModel readGamsInstance(UUID uuid) throws InstanceNotFoundException;
}
