package eu.arrowhead.gams.spi;

import eu.arrowhead.gams.GamsPersistenceService;
import eu.arrowhead.gams.GamsService;
import eu.arrowhead.gams.api.model.GamsInstance;
import eu.arrowhead.gams.api.model.GamsInstanceState;
import eu.arrowhead.gams.api.model.Sensor;
import eu.arrowhead.gams.api.model.request.ModifyGamsInstanceRequest;
import eu.arrowhead.gams.errors.InstanceNotFoundException;
import eu.arrowhead.gams.errors.InvalidModificationException;
import eu.arrowhead.gams.persistence.model.GamsInstanceModel;
import eu.arrowhead.gams.utils.GamsUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GamsServiceImpl implements GamsService {

    private final GamsPersistenceService persistenceService;
    private final ModelMapper mapper;

    @Autowired
    public GamsServiceImpl(final GamsPersistenceService persistenceService, ModelMapper mapper) {
        this.persistenceService = persistenceService;
        this.mapper = mapper;
    }

    private Set<GamsInstance> map(final Iterable<GamsInstanceModel> iterable) {
        final Set<GamsInstance> set = new HashSet<>();
        for (GamsInstanceModel model : iterable) {
            set.add(mapper.map(model, GamsInstance.class));
        }
        return set;
    }

    @Override
    public Set<GamsInstance> searchGamsInstanceByName(final String namePart) {
        return map(persistenceService.searchGamsInstances(namePart));
    }

    @Override
    public Set<GamsInstance> getGamsInstances() {
        return map(persistenceService.getGamsInstances());
    }

    @Override
    public UUID createGamsInstance(final ModifyGamsInstanceRequest instance) {
        GamsUtils.verify(instance);
        final GamsInstanceModel model = mapper.map(instance, GamsInstanceModel.class);
        model.setState(GamsInstanceState.MAINTENANCE);

        return persistenceService.persistGamsInstance(model).getUuid();
    }

    @Override
    public void deleteGamsInstance(UUID uuid) throws InstanceNotFoundException, InvalidModificationException {
        GamsUtils.verify(uuid);
        final GamsInstanceModel model = persistenceService.readGamsInstance(uuid);

        if (model.getState() != GamsInstanceState.MAINTENANCE) {
            throw InvalidModificationException.fromUUID(uuid, model.getState());
        }

        persistenceService.deleteGamsInstance(uuid);
    }

    @Override
    public void updateGamsInstance(UUID uuid, ModifyGamsInstanceRequest instance)
        throws InstanceNotFoundException, InvalidModificationException {
        GamsUtils.verify(uuid);
        GamsUtils.verify(instance);

        final GamsInstanceModel model = persistenceService.readGamsInstance(uuid);

        if (!Objects.equals(model.getName(), instance.getName())) {
            throw InvalidModificationException.fromUUID(uuid);
        }

        model.setDescription(instance.getDescription());
        persistenceService.persistGamsInstance(model);
    }

    @Override
    public GamsInstance getGamsInstance(UUID uuid) throws InstanceNotFoundException {
        GamsUtils.verify(uuid);

        return mapper.map(persistenceService.readGamsInstance(uuid), GamsInstance.class);
    }

    @Override
    public void setGamsInstanceState(UUID uuid, GamsInstanceState newState) throws InstanceNotFoundException {
        final GamsInstanceModel model = persistenceService.readGamsInstance(uuid);
        model.setState(newState);
        persistenceService.persistGamsInstance(model);
    }

    @Override
    public Set<Sensor> getSensors(UUID uuid) throws InstanceNotFoundException {
        return null;
    }
}
