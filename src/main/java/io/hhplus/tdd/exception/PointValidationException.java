package io.hhplus.tdd.exception;

public class PointValidationException extends RuntimeException {
    public PointValidationException() {
        super();
    }

    public PointValidationException(String message) {
        super(message);
    }

    public PointValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PointValidationException(Throwable cause) {
        super(cause);
    }
}
