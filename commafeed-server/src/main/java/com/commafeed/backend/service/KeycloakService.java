package com.commafeed.backend.service;

import com.commafeed.CommaFeedConfiguration;
import com.commafeed.backend.keycloak.DefaultKeycloakClient;
import com.commafeed.backend.keycloak.DefaultKeycloakTokenProvider;
import com.commafeed.backend.keycloak.KeycloakClient;
import com.commafeed.backend.keycloak.KeycloakTokenProvider;
import com.commafeed.backend.keycloak.KeycloakUserMapper;
import com.commafeed.exceptions.KeycloakUserNotFoundException;
import com.commafeed.frontend.model.KeycloakUserDetailResponse;
import com.commafeed.frontend.model.KeycloakUserResponse;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@Singleton
public class KeycloakService {

    private static final int DEFAULT_FIRST = 0;
    private static final int DEFAULT_MAX = 20;
    private static final int MAX_LIMIT = 100;

    private final KeycloakClient client;
    private final KeycloakUserMapper mapper;
    private final KeycloakTokenProvider tokenProvider;

    @Inject
    public KeycloakService(
            KeycloakClient client, KeycloakUserMapper mapper, KeycloakTokenProvider tokenProvider) {
        this.client = client;
        this.mapper = mapper;
        this.tokenProvider = tokenProvider;
    }

    public KeycloakService(CommaFeedConfiguration config) {
        this.tokenProvider = new DefaultKeycloakTokenProvider(config);
        this.client = new DefaultKeycloakClient(config, this.tokenProvider);
        this.mapper = new KeycloakUserMapper();
    }

    public String getAccessToken() {
        return tokenProvider.getAccessToken();
    }

    public List<KeycloakUserResponse> getUsers(String search, Integer first, Integer max) {
        int firstVal = resolveFirst(first);
        int maxVal = resolveMax(max);

        JsonNode usersNode = client.getUsersJson(search, firstVal, maxVal);
        return processUserList(usersNode);
    }

    public KeycloakUserDetailResponse getUserDetails(String keycloakId) {
        validateKeycloakId(keycloakId);

        JsonNode userNode = client.getUserJson(keycloakId);
        ensureRegularUser(userNode, keycloakId);

        KeycloakUserResponse base = mapper.toUserResponse(userNode);
        List<String> roles = client.getRealmRoleNames(keycloakId);
        List<String> groups = client.getGroupNames(keycloakId);

        return mapper.toUserDetailResponse(base, roles, groups);
    }

    private int resolveFirst(Integer first) {
        return (first != null && first >= 0) ? first : DEFAULT_FIRST;
    }

    private int resolveMax(Integer max) {
        if (max == null || max <= 0) {
            return DEFAULT_MAX;
        }
        return Math.min(max, MAX_LIMIT);
    }

    private List<KeycloakUserResponse> processUserList(JsonNode usersNode) {
        if (usersNode == null || !usersNode.isArray()) {
            return Collections.emptyList();
        }

        List<KeycloakUserResponse> result = new ArrayList<>();
        for (JsonNode u : usersNode) {
            if (mapper.isServiceAccount(u)) {
                continue;
            }
            result.add(mapper.toUserResponse(u));
        }
        return result;
    }

    private void validateKeycloakId(String keycloakId) {
        if (StringUtils.isBlank(keycloakId)) {
            throw new KeycloakUserNotFoundException("Keycloak ID cannot be blank");
        }
    }

    private void ensureRegularUser(JsonNode userNode, String keycloakId) {
        if (mapper.isServiceAccount(userNode)) {
            throw new KeycloakUserNotFoundException("Keycloak user not found: " + keycloakId);
        }
    }
}
