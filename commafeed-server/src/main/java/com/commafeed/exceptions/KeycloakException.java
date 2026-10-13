package com.commafeed.exceptions;

public class KeycloakException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public KeycloakException(String message) {
        super(message);
    }

    public KeycloakException(String message, Throwable cause) {
        super(message, cause);
    }
}
