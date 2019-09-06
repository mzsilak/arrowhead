package eu.arrowhead.gams.spi;


import com.google.common.collect.Sets;
import eu.arrowhead.gams.GamsPersistenceService;
import eu.arrowhead.gams.api.model.SensorData;
import eu.arrowhead.gams.errors.InstanceNotFoundException;
import eu.arrowhead.gams.persistence.GamsInstanceRepository;
import eu.arrowhead.gams.persistence.SensorDataRepository;
import eu.arrowhead.gams.persistence.model.GamsInstanceModel;
import eu.arrowhead.gams.persistence.model.SensorDataModel;
import eu.arrowhead.gams.utils.GamsUtils;
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

    private GamsInstanceModel findGamsInstance(final UUID uuid) throws InstanceNotFoundException {
        try {
            final GamsInstanceModel instanceModel = instanceRepository.findOneByUuid(uuid);

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
            final GamsInstanceModel instanceModel = instanceRepository.findOneByUuid(uuid);

            if (Objects.isNull(instanceModel)) {
                throw InstanceNotFoundException.fromUUID(uuid.toString());
            }
        } catch (final EntityNotFoundException e) {
            throw InstanceNotFoundException.fromUUID(uuid.toString());
        }
    }

    @Override
    @Deprecated
    public SensorDataModel mapSensorData(final UUID uuid, final SensorData data) {
        GamsUtils.verify(uuid);
        GamsUtils.verify(data);

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
        GamsUtils.verify(sensorData);
        verifyGamsInstanceExists(sensorData.getUuid());
        return sensorDataRepository.save(sensorData);
    }

    @Override
    public Set<SensorDataModel> querySensorData(final UUID uuid, final Integer size) throws InstanceNotFoundException {
        GamsUtils.verify(uuid);
        Objects.requireNonNull(size, "Query size may not be null");
        verifyGamsInstanceExists(uuid);

        final Page<SensorDataModel> page = sensorDataRepository.findAllByUuid(uuid, PageRequest.of(0, size));
        return Sets.newHashSet(page.getContent());
    }

    @Override
    public Iterable<GamsInstanceModel> searchGamsInstances(String namePart) {
        return instanceRepository.findByNameLike(namePart);
    }

    @Override
    public Iterable<GamsInstanceModel> getGamsInstances() {
        return instanceRepository.findAll();
    }

    @Override
    public GamsInstanceModel persistGamsInstance(final GamsInstanceModel model) {
        GamsUtils.verify(model);
        return instanceRepository.save(model);
    }

    @Override
    public void deleteGamsInstance(final UUID uuid) throws InstanceNotFoundException {
        GamsUtils.verify(uuid);
        instanceRepository.delete(findGamsInstance(uuid));
    }

    @Override
    public GamsInstanceModel readGamsInstance(final UUID uuid) throws InstanceNotFoundException {
        GamsUtils.verify(uuid);
        return findGamsInstance(uuid);
    }
}
