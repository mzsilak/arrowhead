package eu.arrowhead.gams;

import eu.arrowhead.gams.api.model.SensorData;
import eu.arrowhead.gams.errors.InstanceNotFoundException;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

public interface GamsEventsService {

    void dispatch(final UUID uuid, final SensorData data) throws InstanceNotFoundException;

    Set<SensorData> query(final UUID uuid, final Integer size, ZoneId targetZone) throws InstanceNotFoundException;
}
