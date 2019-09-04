package eu.arrowhead.gams.spi;

import com.google.common.collect.Queues;
import eu.arrowhead.gams.GamsPersistenceService;
import eu.arrowhead.gams.GamsEventsService;
import eu.arrowhead.gams.api.model.SensorData;
import eu.arrowhead.gams.errors.InstanceNotFoundException;
import eu.arrowhead.gams.api.model.SensorDataState;
import eu.arrowhead.gams.persistence.model.SensorDataModel;
import eu.arrowhead.gams.utils.GamsUtils;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GamsEventsServiceImpl implements GamsEventsService {

    private final Logger logger = LogManager.getLogger();
    private final GamsPersistenceService persistenceService;
    private final Queue<SensorDataModel> sensorDataQueue;

    @Autowired
    public GamsEventsServiceImpl(final GamsPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
        sensorDataQueue = Queues.newConcurrentLinkedQueue();
    }

    @Override
    public void dispatch(final UUID uuid, final SensorData data) throws InstanceNotFoundException {
        GamsUtils.verify(uuid);
        GamsUtils.verify(data);
        SensorDataModel sensorDataModel = null;
        try {
            sensorDataModel = persistenceService.mapSensorData(uuid, data);
            sensorDataModel = persistenceService.persistSensorData(sensorDataModel);
            sensorDataQueue.add(sensorDataModel);
        } catch (final InstanceNotFoundException inf) {
            if (Objects.nonNull(sensorDataModel)) {
                sensorDataModel.setState(SensorDataState.REJECTED_NOT_FOUND);
                persistenceService.persistSensorData(sensorDataModel);
            }
            throw inf;
        }
    }

    @Override
    public Set<SensorData> query(UUID uuid, Integer size, ZoneId targetZone) throws InstanceNotFoundException {
        GamsUtils.verify(uuid);
        GamsUtils.verifyQuerySize(size);
        GamsUtils.verify(targetZone);

        final Set<SensorDataModel> models = persistenceService.querySensorData(uuid, size);
        final Set<SensorData> set = new HashSet<>();
        for (SensorDataModel model : models) {
            final SensorData data = new SensorData();
            data.setSensorId(model.getSensorId());
            data.setValue(model.getValue());
            data.setMagnitude(model.getMagnitude());
            data.setTimestamp(model.getTimestamp().atZone(targetZone));
            data.setUnit(model.getUnit());
            set.add(data);
        }
        return set;
    }
}
