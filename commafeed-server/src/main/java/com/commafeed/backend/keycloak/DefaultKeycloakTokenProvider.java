package com.commafeed.backend.keycloak;

import com.commafeed.CommaFeedConfiguration;
import com.commafeed.exceptions.KeycloakUpstreamException;
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
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DefaultKeycloakTokenProvider implements KeycloakTokenProvider {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private static final int EXPIRATION_BUFFER_SECONDS = 10;
    private static final int MIN_EXPIRATION_BUFFER_SECONDS = 5;
    private static final int DEFAULT_EXPIRES_IN_SECONDS = 60;

    private final CommaFeedConfiguration config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private String cachedToken;
    private Instant tokenExpiration = Instant.MIN;

    @Inject
    public DefaultKeycloakTokenProvider(CommaFeedConfiguration config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder().connectTimeout(resolveConnectTimeout()).build();
    }

    @Override
    public synchronized String getAccessToken() {
        if (isTokenValid()) {
            return cachedToken;
        }
        return fetchNewToken();
    }

    @Override
    public synchronized void invalidateToken() {
        this.cachedToken = null;
        this.tokenExpiration = Instant.MIN;
    }

    private boolean isTokenValid() {
        return cachedToken != null && Instant.now().isBefore(tokenExpiration);
    }

    private String fetchNewToken() {
        String tokenEndpoint = buildTokenEndpoint();
        String formBody = buildFormBody();

        try {
            HttpRequest request = createTokenRequest(tokenEndpoint, formBody);
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            validateResponseStatus(response.statusCode());
            return extractTokenFromJson(response.body());
        } catch (KeycloakUpstreamException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error connecting to Keycloak token endpoint", e);
            throw new KeycloakUpstreamException("Keycloak authentication error", e);
        }
    }

    private void validateResponseStatus(int statusCode) {
        if (statusCode != 200) {
            log.error(
                    "Failed to authenticate with Keycloak token endpoint. Status code: {}",
                    statusCode);
            throw new KeycloakUpstreamException("Failed to authenticate with Keycloak");
        }
    }

    private HttpRequest createTokenRequest(String endpoint, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(resolveRequestTimeout())
                .build();
    }

    private String extractTokenFromJson(String responseBody) throws Exception {
        JsonNode node = objectMapper.readTree(responseBody);
        String token = node.get("access_token").asText();
        int expiresIn =
                node.has("expires_in")
                        ? node.get("expires_in").asInt()
                        : DEFAULT_EXPIRES_IN_SECONDS;
        updateTokenCache(token, expiresIn);
        return token;
    }

    private void updateTokenCache(String token, int expiresInSeconds) {
        this.cachedToken = token;
        int bufferSeconds =
                Math.max(
                        MIN_EXPIRATION_BUFFER_SECONDS,
                        expiresInSeconds - EXPIRATION_BUFFER_SECONDS);
        this.tokenExpiration = Instant.now().plusSeconds(bufferSeconds);
    }

    private String buildTokenEndpoint() {
        CommaFeedConfiguration.Keycloak kcConfig = config.keycloak();
        String url = trimTrailingSlash(kcConfig.url());
        String realm = kcConfig.realm();
        return String.format("%s/realms/%s/protocol/openid-connect/token", url, realm);
    }

    private String buildFormBody() {
        CommaFeedConfiguration.Keycloak kcConfig = config.keycloak();
        String clientId = kcConfig.clientId();
        String clientSecret = kcConfig.clientSecret().orElse("");
        return String.format(
                "grant_type=client_credentials&client_id=%s&client_secret=%s",
                urlEncode(clientId), urlEncode(clientSecret));
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

    private String urlEncode(String val) {
        if (val == null) {
            return "";
        }
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
