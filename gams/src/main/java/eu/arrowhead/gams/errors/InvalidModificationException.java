package eu.arrowhead.gams.errors;

import eu.arrowhead.gams.api.model.GamsInstanceState;
import java.util.UUID;

public class InvalidModificationException extends GamsException {

    public InvalidModificationException(String message) {
        super(message);
    }

    public InvalidModificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidModificationException(Throwable cause) {
        super(cause);
    }

    public InvalidModificationException(String message, Throwable cause, boolean enableSuppression,
                                        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public static InvalidModificationException fromUUID(final UUID uuid) {
        return new InvalidModificationException("Illegal modification on instance '" + uuid + "'");
    }

    public static InvalidModificationException fromUUID(final UUID uuid, final GamsInstanceState state) {
        return new InvalidModificationException(
            "Unable to delete instance '" + uuid + "' while it is in state '" + state + "'");
    }
}
