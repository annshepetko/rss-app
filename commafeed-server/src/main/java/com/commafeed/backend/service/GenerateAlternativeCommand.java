package com.commafeed.backend.service;

import com.commafeed.frontend.model.request.Target;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record GenerateAlternativeCommand(Long entryId, Target target, String prompt) {}
