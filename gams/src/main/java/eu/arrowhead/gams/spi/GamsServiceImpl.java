package eu.arrowhead.gams.spi;

import com.google.common.collect.Queues;
import eu.arrowhead.gams.GamsPersistenceService;
import eu.arrowhead.gams.GamsService;
import eu.arrowhead.gams.api.model.SensorData;
import eu.arrowhead.gams.errors.InstanceNotFoundException;
import eu.arrowhead.gams.persistence.model.SensorDataState;
import eu.arrowhead.gams.persistence.model.SensorDataModel;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GamsServiceImpl implements GamsService {

    private final Logger logger = LogManager.getLogger();
    private final GamsPersistenceService persistenceService;
    private final Queue<SensorDataModel> sensorDataQueue;

    @Autowired
    public GamsServiceImpl(final GamsPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
        sensorDataQueue = Queues.newConcurrentLinkedQueue();
    }

    @Override
    public void dispatch(final UUID uuid, final SensorData data) throws InstanceNotFoundException {
        SensorDataModel sensorDataModel = null;
        try {
            sensorDataModel = persistenceService.mapSensorData(uuid, data);
            sensorDataModel.setState(SensorDataState.NEW);
            sensorDataModel = persistenceService.persistSensorData(sensorDataModel);
            sensorDataQueue.add(sensorDataModel);
        } catch (final InstanceNotFoundException inf) {
            if (Objects.nonNull(sensorDataModel)) {
                sensorDataModel.setState(SensorDataState.REJECTED);
                persistenceService.persistSensorData(sensorDataModel);
            }
            throw inf;
        }
    }

    @Override
    public Set<SensorDataModel> query(UUID uuid, Integer size) throws InstanceNotFoundException {
        return persistenceService.querySensorData(uuid, size);
    }
}
