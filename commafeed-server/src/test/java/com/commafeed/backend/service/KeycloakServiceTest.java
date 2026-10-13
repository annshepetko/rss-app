package com.commafeed.backend.service;

import com.commafeed.CommaFeedConfiguration;
import com.commafeed.exceptions.KeycloakUpstreamException;
import com.commafeed.exceptions.KeycloakUserNotFoundException;
import com.commafeed.frontend.model.KeycloakUserDetailResponse;
import com.commafeed.frontend.model.KeycloakUserResponse;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class KeycloakServiceTest {

    private HttpServer server;
    private int port;
    private CommaFeedConfiguration config;
    private CommaFeedConfiguration.Keycloak keycloakConfig;
    private KeycloakService service;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.start();

        config = Mockito.mock(CommaFeedConfiguration.class);
        keycloakConfig = Mockito.mock(CommaFeedConfiguration.Keycloak.class);

        Mockito.when(config.keycloak()).thenReturn(keycloakConfig);
        Mockito.when(keycloakConfig.url()).thenReturn("http://localhost:" + port);
        Mockito.when(keycloakConfig.realm()).thenReturn("test-realm");
        Mockito.when(keycloakConfig.clientId()).thenReturn("test-client");
        Mockito.when(keycloakConfig.clientSecret()).thenReturn(Optional.of("secret"));
        Mockito.when(keycloakConfig.connectTimeout()).thenReturn(Duration.ofSeconds(5));
        Mockito.when(keycloakConfig.requestTimeout()).thenReturn(Duration.ofSeconds(5));

        service = new KeycloakService(config);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void setupTokenEndpoint() {
        server.createContext(
                "/realms/test-realm/protocol/openid-connect/token",
                exchange -> {
                    String response = "{\"access_token\":\"mock-token\",\"expires_in\":3600}";
                    byte[] bytes = response.getBytes();
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });
    }

    @Test
    void testGetUsersMappingPaginationAndServiceAccountExclusion() {
        setupTokenEndpoint();

        AtomicInteger roleOrGroupCallCount = new AtomicInteger(0);

        server.createContext(
                "/admin/realms/test-realm/users",
                exchange -> {
                    String query = exchange.getRequestURI().getQuery();
                    Assertions.assertTrue(query.contains("first=0"));
                    Assertions.assertTrue(query.contains("max=20"));
                    Assertions.assertTrue(query.contains("search=john"));

                    String json =
                            """
                [
                  {
                    "id": "uuid-123",
                    "username": "john_doe",
                    "email": "john@example.com",
                    "firstName": "John",
                    "lastName": "Doe",
                    "enabled": true
                  },
                  {
                    "id": "uuid-sa-456",
                    "username": "service-account-test-client",
                    "email": "sa@example.com",
                    "enabled": true,
                    "serviceAccountClientId": "test-client"
                  }
                ]
                """;
                    byte[] bytes = json.getBytes();
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });

        // Track if role-mappings or groups endpoints are called during list request
        server.createContext(
                "/admin/realms/test-realm/users/uuid-123/role-mappings",
                exchange -> {
                    roleOrGroupCallCount.incrementAndGet();
                    exchange.sendResponseHeaders(200, 0);
                    exchange.close();
                });
        server.createContext(
                "/admin/realms/test-realm/users/uuid-123/groups",
                exchange -> {
                    roleOrGroupCallCount.incrementAndGet();
                    exchange.sendResponseHeaders(200, 0);
                    exchange.close();
                });

        List<KeycloakUserResponse> users = service.getUsers("john", 0, 20);

        // Service account should be excluded
        Assertions.assertEquals(1, users.size());
        KeycloakUserResponse user = users.get(0);

        // Keycloak UUID propagation and DTO mapping check
        Assertions.assertEquals("uuid-123", user.getKeycloakId());
        Assertions.assertEquals("john_doe", user.getUsername());
        Assertions.assertEquals("john@example.com", user.getEmail());
        Assertions.assertEquals("John", user.getFirstName());
        Assertions.assertEquals("Doe", user.getLastName());
        Assertions.assertTrue(user.isEnabled());

        // Confirm list requests DO NOT fetch roles or groups per user
        Assertions.assertEquals(
                0,
                roleOrGroupCallCount.get(),
                "List requests must not fetch roles or groups per user");
    }

    @Test
    void testGetUserDetailsWithRolesAndGroups() {
        setupTokenEndpoint();

        server.createContext(
                "/admin/realms/test-realm/users/uuid-123",
                exchange -> {
                    String json =
                            """
                {
                  "id": "uuid-123",
                  "username": "john_doe",
                  "email": "john@example.com",
                  "firstName": "John",
                  "lastName": "Doe",
                  "enabled": true
                }
                """;
                    byte[] bytes = json.getBytes();
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });

        server.createContext(
                "/admin/realms/test-realm/users/uuid-123/role-mappings/realm",
                exchange -> {
                    String json = "[{\"name\":\"admin\"},{\"name\":\"user\"}]";
                    byte[] bytes = json.getBytes();
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });

        server.createContext(
                "/admin/realms/test-realm/users/uuid-123/groups",
                exchange -> {
                    String json = "[{\"name\":\"Engineering\"}]";
                    byte[] bytes = json.getBytes();
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });

        KeycloakUserDetailResponse detail = service.getUserDetails("uuid-123");

        Assertions.assertNotNull(detail);
        Assertions.assertEquals("uuid-123", detail.getKeycloakId());
        Assertions.assertEquals("john_doe", detail.getUsername());
        Assertions.assertEquals(List.of("admin", "user"), detail.getRoles());
        Assertions.assertEquals(List.of("Engineering"), detail.getGroups());
    }

    @Test
    void testMissingUserThrows404Exception() {
        setupTokenEndpoint();

        server.createContext(
                "/admin/realms/test-realm/users/non-existent-uuid",
                exchange -> {
                    exchange.sendResponseHeaders(404, 0);
                    exchange.close();
                });

        Assertions.assertThrows(
                KeycloakUserNotFoundException.class,
                () -> {
                    service.getUserDetails("non-existent-uuid");
                });
    }

    @Test
    void testAuthenticationFailureThrowsUpstreamException() {
        server.createContext(
                "/realms/test-realm/protocol/openid-connect/token",
                exchange -> {
                    exchange.sendResponseHeaders(401, 0);
                    exchange.close();
                });

        Assertions.assertThrows(
                KeycloakUpstreamException.class,
                () -> {
                    service.getUsers(null, 0, 20);
                });
    }

    @Test
    void testFailingServerThrowsUpstreamException() {
        setupTokenEndpoint();

        server.createContext(
                "/admin/realms/test-realm/users",
                exchange -> {
                    exchange.sendResponseHeaders(500, 0);
                    exchange.close();
                });

        Assertions.assertThrows(
                KeycloakUpstreamException.class,
                () -> {
                    service.getUsers(null, 0, 20);
                });
    }
}
