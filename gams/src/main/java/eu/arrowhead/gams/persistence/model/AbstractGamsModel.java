package eu.arrowhead.gams.persistence.model;

import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import org.hibernate.annotations.GenericGenerator;

@MappedSuperclass
public abstract class AbstractGamsModel {

    @Id
    @GeneratedValue
    private Long id;

    @Version
    private Long version;

    @Column(name = "uuid", nullable = false, unique = false, updatable = false, columnDefinition = "BINARY(16)")
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID uuid;

    public AbstractGamsModel() {
        super();
        uuid = UUID.randomUUID();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractGamsModel that = (AbstractGamsModel) o;
        return id.equals(that.id) && version.equals(that.version) && uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version, uuid);
    }

    protected abstract void appendToString(final StringBuilder sb);

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" [id=").append(id);
        sb.append(", version=").append(version);
        sb.append(", uuid=").append(uuid);
        appendToString(sb);
        sb.append(']');
        return sb.toString();
    }
}
