package com.commafeed.exceptions;

public class KeycloakUserNotFoundException extends KeycloakException {

    private static final long serialVersionUID = 1L;

    public KeycloakUserNotFoundException(String message) {
        super(message);
    }
}
