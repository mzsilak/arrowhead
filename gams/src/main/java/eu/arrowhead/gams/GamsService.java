package eu.arrowhead.gams;

import eu.arrowhead.gams.api.model.SensorData;
import eu.arrowhead.gams.errors.InstanceNotFoundException;
import eu.arrowhead.gams.persistence.model.SensorDataModel;
import java.util.Set;
import java.util.UUID;

public interface GamsService {

    void dispatch(final UUID uuid, final SensorData data) throws InstanceNotFoundException;

    Set<SensorDataModel> query(final UUID uuid, final Integer size) throws InstanceNotFoundException;
}
