package eu.arrowhead.gams.persistence;

import eu.arrowhead.gams.persistence.model.GamsInstanceModel;
import org.springframework.stereotype.Repository;

@Repository
public interface GamsInstanceRepository extends AbstractGamsRepository<GamsInstanceModel> {

    Iterable<GamsInstanceModel> findByNameLike(String namePart);
}
