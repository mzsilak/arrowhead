package eu.arrowhead.gams.persistence;

import eu.arrowhead.gams.persistence.model.SensorDataModel;
import eu.arrowhead.gams.api.model.SensorDataState;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface SensorDataRepository extends AbstractGamsRepository<SensorDataModel> {

    Set<SensorDataModel> findAllByUuid(final UUID uuid);

    Set<SensorDataModel> findAllByState(final SensorDataState state);

    Page<SensorDataModel> findAllByUuid(final UUID uuid, final Pageable pageable);
}
