package com.commafeed.exceptions;

public class LLMException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LLMException(String message) {
        super(message);
    }

    public LLMException(String message, Throwable cause) {
        super(message, cause);
    }
}
