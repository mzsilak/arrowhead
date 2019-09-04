package eu.arrowhead.gams.api.model.request;

import eu.arrowhead.gams.api.model.GamsInstanceState;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Objects;

@ApiModel
public class ModifyGamsInstanceStateRequest {

    @ApiModelProperty(notes = "The new state of the instance.", required = true)
    private GamsInstanceState newState;

    public ModifyGamsInstanceStateRequest() {
        super();
    }

    public GamsInstanceState getNewState() {
        return newState;
    }

    public void setNewState(GamsInstanceState newState) {
        this.newState = newState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ModifyGamsInstanceStateRequest that = (ModifyGamsInstanceStateRequest) o;
        return newState == that.newState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(newState);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("[newState='").append(newState).append("']");
        return sb.toString();
    }
}
