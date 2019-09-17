package com.amazon.disco.agent;

/**
 * Thrown when there is an issue with HTTP communication between Alpha One components
 */
public class AlphaOneHttpException extends Exception {
    private static final long serialVersionUID = -128389147239848L;

    /**
     * Constructs new instance of this exception, with specified message.
     *
     * @param message the message describing exception
     */
    public AlphaOneHttpException(String message) {
        super(message);
    }

    /**
     * Constructs new instance of this exception, with specified message and cause
     *
     * @param message the message describing exception
     * @param cause the cause of this exception
     */
    public AlphaOneHttpException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs new instance of this exception, with specified cause
     *
     * @param cause the cause of this exception
     */
    public AlphaOneHttpException(Throwable cause) {
        super(cause);
    }
}
