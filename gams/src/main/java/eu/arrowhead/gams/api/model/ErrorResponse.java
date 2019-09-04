package eu.arrowhead.gams.api.model;

import eu.arrowhead.gams.errors.InstanceNotFoundException;

public class ErrorResponse {

    private String message;

    public ErrorResponse(String message) {
        this.message = message;
    }

    public static ErrorResponse from(Exception e) {
        return new ErrorResponse(e.getMessage());
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
