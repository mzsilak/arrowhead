package eu.arrowhead.gams.errors;

public class InstanceNotFoundException extends GamsException {

    public InstanceNotFoundException(String message) {
        super(message);
    }

    public InstanceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public InstanceNotFoundException(Throwable cause) {
        super(cause);
    }

    public InstanceNotFoundException(String message, Throwable cause, boolean enableSuppression,
                                     boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public static InstanceNotFoundException fromUUID(final String uuid) {
        return new InstanceNotFoundException("Unable to find instance '" + uuid + "'");
    }
}
