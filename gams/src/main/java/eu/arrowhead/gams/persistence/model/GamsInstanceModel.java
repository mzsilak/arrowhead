package eu.arrowhead.gams.persistence.model;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
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
    private LocalDateTime creationDate;

    @Column(name = "state", unique = false, updatable = true)
    private GamsInstanceState state;

    public GamsInstanceModel() {
        super();
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

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    protected void appendToString(StringBuilder sb) {
        sb.append(", name=").append(name);
        sb.append(", description=").append(description);
        sb.append(", creationDate=").append(creationDate);
    }
}
