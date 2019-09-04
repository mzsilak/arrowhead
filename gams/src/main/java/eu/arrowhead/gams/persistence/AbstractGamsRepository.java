package eu.arrowhead.gams.persistence;

import eu.arrowhead.gams.persistence.model.AbstractGamsModel;
import eu.arrowhead.gams.persistence.model.GamsInstanceModel;
import java.util.UUID;
import javax.persistence.EntityNotFoundException;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface AbstractGamsRepository<T extends AbstractGamsModel> extends CrudRepository<T, Long> {

    T findOneByUuid(final UUID uuid) throws EntityNotFoundException;
}
