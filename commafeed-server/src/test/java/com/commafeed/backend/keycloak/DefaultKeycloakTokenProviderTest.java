package com.commafeed.backend.keycloak;

import com.commafeed.CommaFeedConfiguration;
import com.commafeed.exceptions.KeycloakUpstreamException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DefaultKeycloakTokenProviderTest {

    private HttpServer server;
    private int port;
    private CommaFeedConfiguration config;
    private CommaFeedConfiguration.Keycloak keycloakConfig;
    private DefaultKeycloakTokenProvider provider;

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

        provider = new DefaultKeycloakTokenProvider(config);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testGetAccessTokenCachesToken() {
        AtomicInteger requestCount = new AtomicInteger(0);

        server.createContext(
                "/realms/test-realm/protocol/openid-connect/token",
                exchange -> {
                    requestCount.incrementAndGet();
                    String response =
                            "{\"access_token\":\"cached-token-value\",\"expires_in\":3600}";
                    byte[] bytes = response.getBytes();
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });

        String token1 = provider.getAccessToken();
        String token2 = provider.getAccessToken();

        Assertions.assertEquals("cached-token-value", token1);
        Assertions.assertEquals("cached-token-value", token2);
        Assertions.assertEquals(
                1, requestCount.get(), "Should cache token and not call endpoint again");

        provider.invalidateToken();
        String token3 = provider.getAccessToken();
        Assertions.assertEquals("cached-token-value", token3);
        Assertions.assertEquals(
                2,
                requestCount.get(),
                "Invalidating token should cause next call to fetch new token");
    }

    @Test
    void testAuthenticationFailureThrowsKeycloakUpstreamException() {
        server.createContext(
                "/realms/test-realm/protocol/openid-connect/token",
                exchange -> {
                    exchange.sendResponseHeaders(401, 0);
                    exchange.close();
                });

        Assertions.assertThrows(KeycloakUpstreamException.class, () -> provider.getAccessToken());
    }
}
