package com.commafeed.backend.exception;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record LLMErrorResponse(String message) {}
