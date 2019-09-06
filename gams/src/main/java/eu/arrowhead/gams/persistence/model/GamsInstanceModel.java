package eu.arrowhead.gams.persistence.model;

import eu.arrowhead.gams.api.model.GamsInstanceState;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "gams_instance", uniqueConstraints = @UniqueConstraint(columnNames = {"uuid"}))
public class GamsInstanceModel extends AbstractGamsModel {

    @Column(name = "name", unique = true, updatable = true)
    private String name;

    @Column(name = "description", unique = false, length = 2048)
    private String description;

    @Column(name = "creation_date", unique = false, updatable = false)
    private ZonedDateTime creationDate;

    @Column(name = "state", unique = false, updatable = true)
    @Enumerated(EnumType.STRING)
    private GamsInstanceState state;

    public GamsInstanceModel() {
        super();
    }

    public GamsInstanceModel(String name, String description, ZonedDateTime creationDate, GamsInstanceState state) {
        this.name = name;
        this.description = description;
        this.creationDate = creationDate;
        this.state = state;
    }

    public GamsInstanceModel(UUID uuid, String name, String description, ZonedDateTime creationDate,
                             GamsInstanceState state) {
        this.name = name;
        this.description = description;
        this.creationDate = creationDate;
        this.state = state;
        setUuid(uuid);
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
    protected void appendToString(StringBuilder sb) {
        sb.append(", name=").append(name);
        sb.append(", description=").append(description);
        sb.append(", creationDate=").append(creationDate);
    }
}
