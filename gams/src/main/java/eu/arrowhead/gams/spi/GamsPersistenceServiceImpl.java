package eu.arrowhead.gams.spi;

import com.google.common.collect.Sets;
import eu.arrowhead.gams.GamsPersistenceService;
import eu.arrowhead.gams.api.model.SensorData;
import eu.arrowhead.gams.errors.InstanceNotFoundException;
import eu.arrowhead.gams.persistence.GamsInstanceRepository;
import eu.arrowhead.gams.persistence.SensorDataRepository;
import eu.arrowhead.gams.persistence.model.GamsInstanceModel;
import eu.arrowhead.gams.persistence.model.SensorDataModel;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class GamsPersistenceServiceImpl implements GamsPersistenceService {

    private final SensorDataRepository sensorDataRepository;
    private final GamsInstanceRepository instanceRepository;

    public GamsPersistenceServiceImpl(SensorDataRepository sensorDataRepository,
                                      GamsInstanceRepository instanceRepository) {
        this.sensorDataRepository = sensorDataRepository;
        this.instanceRepository = instanceRepository;
    }

    private void verify(final UUID uuid) {
        Objects.requireNonNull(uuid, "UUID must not be null");
    }

    private void verify(final SensorData sensorData) {
        Objects.requireNonNull(sensorData, "SensorData must not be null");
    }

    private void verify(final SensorDataModel sensorDataModel) {
        Objects.requireNonNull(sensorDataModel, "SensorDataModel must not be null");
    }

    private GamsInstanceModel findGamsInstance(final UUID uuid) throws InstanceNotFoundException {
        try {
            final GamsInstanceModel instanceModel = instanceRepository.findOneByUUID(uuid);

            if (Objects.isNull(instanceModel)) {
                throw InstanceNotFoundException.fromUUID(uuid.toString());
            }
            return instanceModel;
        } catch (final EntityNotFoundException e) {
            throw InstanceNotFoundException.fromUUID(uuid.toString());
        }
    }

    private void verifyGamsInstanceExists(final UUID uuid) throws InstanceNotFoundException {
        try {
            final GamsInstanceModel instanceModel = instanceRepository.findOneByUUID(uuid);

            if (Objects.isNull(instanceModel)) {
                throw InstanceNotFoundException.fromUUID(uuid.toString());
            }
        } catch (final EntityNotFoundException e) {
            throw InstanceNotFoundException.fromUUID(uuid.toString());
        }
    }

    @Override
    public SensorDataModel mapSensorData(final UUID uuid, final SensorData data) {
        verify(uuid);
        verify(data);

        final LocalDateTime timestamp = LocalDateTime.ofInstant(data.getTimestamp().toInstant(), ZoneOffset.UTC);
        final SensorDataModel model = new SensorDataModel();
        model.setUuid(uuid);
        model.setSensorId(data.getSensorId());
        model.setTimestamp(timestamp);
        model.setValue(data.getValue());
        model.setMagnitude(data.getMagnitude());
        model.setUnit(data.getUnit());

        return model;
    }

    @Override
    public SensorDataModel persistSensorData(final SensorDataModel sensorData) throws InstanceNotFoundException {
        verify(sensorData);
        verifyGamsInstanceExists(sensorData.getUuid());
        return sensorDataRepository.save(sensorData);
    }

    @Override
    public Set<SensorDataModel> querySensorData(UUID uuid, Integer size) throws InstanceNotFoundException {
        verify(uuid);
        Objects.requireNonNull(size, "Query size may not be null");
        verifyGamsInstanceExists(uuid);

        final Page<SensorDataModel> page = sensorDataRepository.findAllByUuid(uuid, PageRequest.of(0, size));
        return Sets.newHashSet(page.getContent());
    }
}
