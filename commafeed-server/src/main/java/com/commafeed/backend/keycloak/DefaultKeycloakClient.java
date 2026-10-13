package com.commafeed.backend.keycloak;

import com.commafeed.CommaFeedConfiguration;
import com.commafeed.exceptions.KeycloakUpstreamException;
import com.commafeed.exceptions.KeycloakUserNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@Singleton
public class DefaultKeycloakClient implements KeycloakClient {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    private final CommaFeedConfiguration config;
    private final KeycloakTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Inject
    public DefaultKeycloakClient(
            CommaFeedConfiguration config, KeycloakTokenProvider tokenProvider) {
        this.config = config;
        this.tokenProvider = tokenProvider;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder().connectTimeout(resolveConnectTimeout()).build();
    }

    @Override
    public JsonNode getUsersJson(String search, int first, int max) {
        String endpoint = buildUsersEndpoint(search, first, max);
        return executeGet(endpoint, false);
    }

    @Override
    public JsonNode getUserJson(String keycloakId) {
        String endpoint = buildUserEndpoint(keycloakId);
        return executeGet(endpoint, true);
    }

    @Override
    public List<String> getRealmRoleNames(String keycloakId) {
        String endpoint = buildRolesEndpoint(keycloakId);
        return extractNames(endpoint, "name");
    }

    @Override
    public List<String> getGroupNames(String keycloakId) {
        String endpoint = buildGroupsEndpoint(keycloakId);
        return extractNames(endpoint, "name");
    }

    private JsonNode executeGet(String endpoint, boolean checkNotFound) {
        try {
            HttpRequest request = buildAuthorizedRequest(endpoint);
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            handleHttpStatus(response.statusCode(), endpoint, checkNotFound);
            return objectMapper.readTree(response.body());
        } catch (KeycloakUserNotFoundException | KeycloakUpstreamException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error communicating with Keycloak endpoint {}", endpoint, e);
            throw new KeycloakUpstreamException("Unable to communicate with Keycloak", e);
        }
    }

    private void handleHttpStatus(int status, String endpoint, boolean checkNotFound) {
        if (status == 404 && checkNotFound) {
            throw new KeycloakUserNotFoundException("Keycloak resource not found at " + endpoint);
        }

        if (status == 401 || status == 403) {
            log.warn("Keycloak authorization failure (status {}). Invalidating token.", status);
            tokenProvider.invalidateToken();
            throw new KeycloakUpstreamException("Keycloak authorization failure");
        }

        if (status != 200) {
            log.error("Keycloak returned unexpected status {} for endpoint {}", status, endpoint);
            throw new KeycloakUpstreamException("Keycloak returned status " + status);
        }
    }

    private List<String> extractNames(String endpoint, String fieldName) {
        try {
            HttpRequest request = buildAuthorizedRequest(endpoint);
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode array = objectMapper.readTree(response.body());
                return parseStringList(array, fieldName);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch '{}' from endpoint {}", fieldName, endpoint, e);
        }
        return Collections.emptyList();
    }

    private List<String> parseStringList(JsonNode array, String fieldName) {
        if (!array.isArray()) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        for (JsonNode item : array) {
            if (item.has(fieldName) && !item.get(fieldName).isNull()) {
                list.add(item.get(fieldName).asText());
            }
        }
        return list;
    }

    private HttpRequest buildAuthorizedRequest(String endpoint) {
        String token = tokenProvider.getAccessToken();
        return HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .timeout(resolveRequestTimeout())
                .build();
    }

    private String buildUsersEndpoint(String search, int first, int max) {
        StringBuilder query = new StringBuilder();
        query.append(String.format("first=%d&max=%d", first, max));
        if (StringUtils.isNotBlank(search)) {
            query.append("&search=").append(urlEncode(search.trim()));
        }
        return String.format("%s/admin/realms/%s/users?%s", getBaseUrl(), getRealm(), query);
    }

    private String buildUserEndpoint(String keycloakId) {
        return String.format(
                "%s/admin/realms/%s/users/%s", getBaseUrl(), getRealm(), urlEncode(keycloakId));
    }

    private String buildRolesEndpoint(String keycloakId) {
        return String.format(
                "%s/admin/realms/%s/users/%s/role-mappings/realm",
                getBaseUrl(), getRealm(), urlEncode(keycloakId));
    }

    private String buildGroupsEndpoint(String keycloakId) {
        return String.format(
                "%s/admin/realms/%s/users/%s/groups",
                getBaseUrl(), getRealm(), urlEncode(keycloakId));
    }

    private String getBaseUrl() {
        return trimTrailingSlash(config.keycloak().url());
    }

    private String getRealm() {
        return config.keycloak().realm();
    }

    private Duration resolveConnectTimeout() {
        if (config != null && config.keycloak() != null) {
            try {
                Duration timeout = config.keycloak().connectTimeout();
                if (timeout != null) {
                    return timeout;
                }
            } catch (Exception ignored) {
            }
        }
        return DEFAULT_TIMEOUT;
    }

    private Duration resolveRequestTimeout() {
        if (config != null && config.keycloak() != null) {
            try {
                Duration timeout = config.keycloak().requestTimeout();
                if (timeout != null) {
                    return timeout;
                }
            } catch (Exception ignored) {
            }
        }
        return DEFAULT_TIMEOUT;
    }

    private String trimTrailingSlash(String str) {
        if (str == null) {
            return "";
        }
        return str.endsWith("/") ? str.substring(0, str.length() - 1) : str;
    }

    private String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
