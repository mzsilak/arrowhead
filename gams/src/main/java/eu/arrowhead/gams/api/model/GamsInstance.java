package eu.arrowhead.gams.api.model;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

public class GamsInstance {

    private UUID uuid;
    private String name;
    private String description;
    private ZonedDateTime creationDate;
    private GamsInstanceState state;

    public GamsInstance() {
        super();
    }

    public GamsInstance(String name, String description, ZonedDateTime creationDate, GamsInstanceState state) {
        this.name = name;
        this.description = description;
        this.creationDate = creationDate;
        this.state = state;
    }

    public GamsInstance(UUID uuid, String name, String description, ZonedDateTime creationDate,
                        GamsInstanceState state) {
        this.uuid = uuid;
        this.name = name;
        this.description = description;
        this.creationDate = creationDate;
        this.state = state;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ZonedDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(ZonedDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public GamsInstanceState getState() {
        return state;
    }

    public void setState(GamsInstanceState state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GamsInstance instance = (GamsInstance) o;
        return Objects.equals(uuid, instance.uuid) && Objects.equals(creationDate, instance.creationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, creationDate);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GamsInstance[");
        sb.append("uuid=").append(uuid);
        sb.append(", name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", creationDate=").append(creationDate);
        sb.append(", state=").append(state);
        sb.append(']');
        return sb.toString();
    }
}
