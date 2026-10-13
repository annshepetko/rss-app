package com.commafeed.exceptions;

public class KeycloakUpstreamException extends KeycloakException {

    private static final long serialVersionUID = 1L;

    public KeycloakUpstreamException(String message) {
        super(message);
    }

    public KeycloakUpstreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
