package com.commafeed.backend.keycloak;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public interface KeycloakClient {

    JsonNode getUsersJson(String search, int first, int max);

    JsonNode getUserJson(String keycloakId);

    List<String> getRealmRoleNames(String keycloakId);

    List<String> getGroupNames(String keycloakId);
}
