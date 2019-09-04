package eu.arrowhead.gams;

import eu.arrowhead.gams.api.model.GamsInstance;
import eu.arrowhead.gams.api.model.GamsInstanceState;
import eu.arrowhead.gams.api.model.request.ModifyGamsInstanceRequest;
import eu.arrowhead.gams.errors.InstanceNotFoundException;
import eu.arrowhead.gams.errors.InvalidModificationException;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

public interface GamsService {

    Set<GamsInstance> list(final ZoneId targetZone);

    UUID create(final ModifyGamsInstanceRequest instance);

    void delete(final UUID uuid) throws InstanceNotFoundException, InvalidModificationException;

    void update(final UUID uuid, final ModifyGamsInstanceRequest instance)
        throws InstanceNotFoundException, InvalidModificationException;

    void setState(final UUID uuid, final GamsInstanceState state) throws InstanceNotFoundException;

    GamsInstance read(final UUID uuid, final ZoneId targetZone) throws InstanceNotFoundException;

    Set<GamsInstance> searchByName(final String namePart, final ZoneId targetZone);
}
