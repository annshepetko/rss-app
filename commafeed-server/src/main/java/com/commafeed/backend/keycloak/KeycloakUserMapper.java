package com.commafeed.backend.keycloak;

import com.commafeed.frontend.model.KeycloakUserDetailResponse;
import com.commafeed.frontend.model.KeycloakUserResponse;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Singleton;
import java.util.List;

@Singleton
public class KeycloakUserMapper {

    private static final String SERVICE_ACCOUNT_PREFIX = "service-account-";

    public boolean isServiceAccount(JsonNode userNode) {
        if (userNode == null) {
            return false;
        }
        return hasServiceAccountUsername(userNode) || hasServiceAccountClientId(userNode);
    }

    public KeycloakUserResponse toUserResponse(JsonNode node) {
        if (node == null) {
            return null;
        }
        return KeycloakUserResponse.builder()
                .keycloakId(extractString(node, "id", null))
                .username(extractString(node, "username", ""))
                .email(extractString(node, "email", null))
                .firstName(extractString(node, "firstName", null))
                .lastName(extractString(node, "lastName", null))
                .enabled(extractBoolean(node, "enabled", true))
                .build();
    }

    public KeycloakUserDetailResponse toUserDetailResponse(
            KeycloakUserResponse base, List<String> roles, List<String> groups) {
        return KeycloakUserDetailResponse.builder()
                .keycloakId(base.getKeycloakId())
                .username(base.getUsername())
                .email(base.getEmail())
                .firstName(base.getFirstName())
                .lastName(base.getLastName())
                .enabled(base.isEnabled())
                .roles(roles)
                .groups(groups)
                .build();
    }

    private boolean hasServiceAccountUsername(JsonNode node) {
        if (node.has("username")) {
            String username = node.get("username").asText();
            return username != null && username.startsWith(SERVICE_ACCOUNT_PREFIX);
        }
        return false;
    }

    private boolean hasServiceAccountClientId(JsonNode node) {
        return node.has("serviceAccountClientId") && !node.get("serviceAccountClientId").isNull();
    }

    private String extractString(JsonNode node, String fieldName, String defaultValue) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asText();
        }
        return defaultValue;
    }

    private boolean extractBoolean(JsonNode node, String fieldName, boolean defaultValue) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asBoolean();
        }
        return defaultValue;
    }
}
