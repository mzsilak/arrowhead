package eu.arrowhead.gams.errors;

public class GamsException extends Exception {

    public GamsException(String message) {
        super(message);
    }

    public GamsException(String message, Throwable cause) {
        super(message, cause);
    }

    public GamsException(Throwable cause) {
        super(cause);
    }

    public GamsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
