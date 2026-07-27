package com.commafeed.backend.service;

public interface LLMClient {

    String generateAlternative(String prompt, String originalText);
}
