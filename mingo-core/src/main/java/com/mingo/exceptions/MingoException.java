package com.mingo.exceptions;


public class MingoException extends RuntimeException {

    /**
     * Default constructor.
     */
    public MingoException() {
    }

    /**
     * Constructor with parameters.
     *
     * @param message message
     */
    public MingoException(String message) {
        super(message);
    }

    /**
     * Constructor with parameters.
     *
     * @param cause cause
     */
    public MingoException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with parameters.
     *
     * @param message message
     * @param cause   cause
     */
    public MingoException(String message, Throwable cause) {
        super(message, cause);
    }
}