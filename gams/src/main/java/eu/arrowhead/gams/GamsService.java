package eu.arrowhead.gams;

import eu.arrowhead.gams.api.model.GamsInstance;
import eu.arrowhead.gams.api.model.GamsInstanceState;
import eu.arrowhead.gams.api.model.Sensor;
import eu.arrowhead.gams.api.model.request.ModifyGamsInstanceRequest;
import eu.arrowhead.gams.errors.InstanceNotFoundException;
import eu.arrowhead.gams.errors.InvalidModificationException;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

public interface GamsService {

    Set<GamsInstance> getGamsInstances();

    UUID createGamsInstance(final ModifyGamsInstanceRequest instance);

    void deleteGamsInstance(final UUID uuid) throws InstanceNotFoundException, InvalidModificationException;

    void updateGamsInstance(final UUID uuid, final ModifyGamsInstanceRequest instance)
        throws InstanceNotFoundException, InvalidModificationException;

    void setGamsInstanceState(final UUID uuid, final GamsInstanceState state) throws InstanceNotFoundException;

    GamsInstance getGamsInstance(final UUID uuid) throws InstanceNotFoundException;

    Set<GamsInstance> searchGamsInstanceByName(final String namePart);

    Set<Sensor> getSensors(final UUID uuid) throws InstanceNotFoundException;
}
