package com.commafeed.backend.keycloak;

public interface KeycloakTokenProvider {

    String getAccessToken();

    void invalidateToken();
}
