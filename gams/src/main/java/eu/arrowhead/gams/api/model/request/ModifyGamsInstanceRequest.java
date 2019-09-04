package eu.arrowhead.gams.api.model.request;

import eu.arrowhead.gams.api.model.GamsInstance;
import eu.arrowhead.gams.api.model.GamsInstanceState;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

@ApiModel
public class ModifyGamsInstanceRequest {

    @ApiModelProperty(notes = "The name of the new instance.", required = true)
    private String name;

    @ApiModelProperty(notes = "A description with maximum 2048 characters.", required = false)
    private String description;

    @ApiModelProperty(hidden = true)
    private ZonedDateTime creationDate;

    public ModifyGamsInstanceRequest() {
        super();
        creationDate = ZonedDateTime.now(ZoneOffset.UTC);
    }

    public GamsInstance toModel() {
        return new GamsInstance(name, description, creationDate, GamsInstanceState.MAINTENANCE);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ModifyGamsInstanceRequest that = (ModifyGamsInstanceRequest) o;
        return name.equals(that.name) && Objects.equals(description, that.description) && creationDate
            .equals(that.creationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, creationDate);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("[name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", creationDate=").append(creationDate);
        sb.append(']');
        return sb.toString();
    }
}
