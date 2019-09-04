package eu.arrowhead.gams.spi;

import eu.arrowhead.gams.GamsPersistenceService;
import eu.arrowhead.gams.GamsService;
import eu.arrowhead.gams.api.model.GamsInstance;
import eu.arrowhead.gams.api.model.GamsInstanceState;
import eu.arrowhead.gams.api.model.request.ModifyGamsInstanceRequest;
import eu.arrowhead.gams.errors.InstanceNotFoundException;
import eu.arrowhead.gams.errors.InvalidModificationException;
import eu.arrowhead.gams.persistence.model.GamsInstanceModel;
import eu.arrowhead.gams.utils.GamsUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GamsServiceImpl implements GamsService {

    private final GamsPersistenceService persistenceService;

    @Autowired
    public GamsServiceImpl(final GamsPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    private GamsInstance map(final GamsInstanceModel model, final ZoneId targetZone) {
        return new GamsInstance(model.getUuid(), model.getName(), model.getDescription(),
                                model.getCreationDate().atZone(targetZone), model.getState());
    }

    private GamsInstanceModel map(final GamsInstance instance) {
        final LocalDateTime timestamp = LocalDateTime.ofInstant(instance.getCreationDate().toInstant(), ZoneOffset.UTC);
        return new GamsInstanceModel(instance.getUuid(), instance.getName(), instance.getDescription(), timestamp,
                                     instance.getState());
    }

    private Set<GamsInstance> map(final Iterable<GamsInstanceModel> iterable, final ZoneId targetZone) {
        final Set<GamsInstance> set = new HashSet<>();
        for (GamsInstanceModel model : iterable) {
            set.add(map(model, targetZone));
        }
        return set;
    }

    @Override
    public Set<GamsInstance> searchByName(final String namePart, final ZoneId targetZone) {
        GamsUtils.verify(targetZone);
        return map(persistenceService.searchGamsInstances(namePart), targetZone);
    }

    @Override
    public Set<GamsInstance> list(final ZoneId targetZone) {
        GamsUtils.verify(targetZone);
        return map(persistenceService.getGamsInstances(), targetZone);
    }

    @Override
    public UUID create(final ModifyGamsInstanceRequest instance) {
        GamsUtils.verify(instance);
        final LocalDateTime timestamp = LocalDateTime.ofInstant(instance.getCreationDate().toInstant(), ZoneOffset.UTC);
        final GamsInstanceModel model = new GamsInstanceModel();
        model.setName(instance.getName());
        model.setDescription(instance.getDescription());
        model.setCreationDate(timestamp);
        model.setState(GamsInstanceState.MAINTENANCE);
        model.setUuid(UUID.randomUUID());

        return persistenceService.persistGamsInstance(model);
    }

    @Override
    public void delete(UUID uuid) throws InstanceNotFoundException, InvalidModificationException {
        GamsUtils.verify(uuid);
        final GamsInstanceModel model = persistenceService.readGamsInstance(uuid);

        if (model.getState() != GamsInstanceState.MAINTENANCE) {
            throw InvalidModificationException.fromUUID(uuid, model.getState());
        }

        persistenceService.deleteGamsInstance(uuid);
    }

    @Override
    public void update(UUID uuid, ModifyGamsInstanceRequest instance)
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
    public GamsInstance read(UUID uuid, final ZoneId targetZone) throws InstanceNotFoundException {
        GamsUtils.verify(uuid);
        GamsUtils.verify(targetZone);

        final GamsInstanceModel model = persistenceService.readGamsInstance(uuid);
        return map(model, targetZone);
    }

    @Override
    public void setState(UUID uuid, GamsInstanceState newState) throws InstanceNotFoundException {
        final GamsInstanceModel model = persistenceService.readGamsInstance(uuid);
        model.setState(newState);
        persistenceService.persistGamsInstance(model);
    }
}
