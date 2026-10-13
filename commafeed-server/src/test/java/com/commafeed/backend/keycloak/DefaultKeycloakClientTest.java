package com.commafeed.backend.keycloak;

import com.commafeed.CommaFeedConfiguration;
import com.commafeed.exceptions.KeycloakUpstreamException;
import com.commafeed.exceptions.KeycloakUserNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DefaultKeycloakClientTest {

    private HttpServer server;
    private int port;
    private CommaFeedConfiguration config;
    private CommaFeedConfiguration.Keycloak keycloakConfig;
    private KeycloakTokenProvider tokenProvider;
    private DefaultKeycloakClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.start();

        config = Mockito.mock(CommaFeedConfiguration.class);
        keycloakConfig = Mockito.mock(CommaFeedConfiguration.Keycloak.class);
        tokenProvider = Mockito.mock(KeycloakTokenProvider.class);

        Mockito.when(config.keycloak()).thenReturn(keycloakConfig);
        Mockito.when(keycloakConfig.url()).thenReturn("http://localhost:" + port);
        Mockito.when(keycloakConfig.realm()).thenReturn("test-realm");
        Mockito.when(keycloakConfig.clientId()).thenReturn("test-client");
        Mockito.when(keycloakConfig.clientSecret()).thenReturn(Optional.of("secret"));
        Mockito.when(keycloakConfig.connectTimeout()).thenReturn(Duration.ofSeconds(5));
        Mockito.when(keycloakConfig.requestTimeout()).thenReturn(Duration.ofSeconds(5));

        Mockito.when(tokenProvider.getAccessToken()).thenReturn("test-access-token");

        client = new DefaultKeycloakClient(config, tokenProvider);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testGetUsersJsonAttachesBearerToken() {
        AtomicBoolean tokenVerified = new AtomicBoolean(false);

        server.createContext(
                "/admin/realms/test-realm/users",
                exchange -> {
                    String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                    if ("Bearer test-access-token".equals(authHeader)) {
                        tokenVerified.set(true);
                    }
                    String response = "[{\"id\":\"uuid-1\",\"username\":\"alice\"}]";
                    byte[] bytes = response.getBytes();
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });

        JsonNode users = client.getUsersJson(null, 0, 10);
        Assertions.assertTrue(
                tokenVerified.get(), "Must send Bearer token in Authorization header");
        Assertions.assertTrue(users.isArray());
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("alice", users.get(0).get("username").asText());
    }

    @Test
    void testGetUserJsonNotFoundThrowsKeycloakUserNotFoundException() {
        server.createContext(
                "/admin/realms/test-realm/users/missing-id",
                exchange -> {
                    exchange.sendResponseHeaders(404, 0);
                    exchange.close();
                });

        Assertions.assertThrows(
                KeycloakUserNotFoundException.class, () -> client.getUserJson("missing-id"));
    }

    @Test
    void testUnauthorizedInvalidatesTokenAndThrowsUpstreamException() {
        server.createContext(
                "/admin/realms/test-realm/users/uuid-1",
                exchange -> {
                    exchange.sendResponseHeaders(401, 0);
                    exchange.close();
                });

        Assertions.assertThrows(
                KeycloakUpstreamException.class, () -> client.getUserJson("uuid-1"));
        Mockito.verify(tokenProvider).invalidateToken();
    }

    @Test
    void testGetRolesAndGroupsExtraction() {
        server.createContext(
                "/admin/realms/test-realm/users/uuid-1/role-mappings/realm",
                exchange -> {
                    String response = "[{\"name\":\"admin\"},{\"name\":\"user\"}]";
                    byte[] bytes = response.getBytes();
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });

        server.createContext(
                "/admin/realms/test-realm/users/uuid-1/groups",
                exchange -> {
                    String response = "[{\"name\":\"Engineering\"}]";
                    byte[] bytes = response.getBytes();
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });

        List<String> roles = client.getRealmRoleNames("uuid-1");
        List<String> groups = client.getGroupNames("uuid-1");

        Assertions.assertEquals(List.of("admin", "user"), roles);
        Assertions.assertEquals(List.of("Engineering"), groups);
    }
}
