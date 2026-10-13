package com.commafeed.backend.keycloak;

import com.commafeed.frontend.model.KeycloakUserDetailResponse;
import com.commafeed.frontend.model.KeycloakUserResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KeycloakUserMapperTest {

    private KeycloakUserMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mapper = new KeycloakUserMapper();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testIsServiceAccount() throws Exception {
        JsonNode regularUser =
                objectMapper.readTree("{\"username\":\"alice\",\"email\":\"alice@example.com\"}");
        Assertions.assertFalse(mapper.isServiceAccount(regularUser));

        JsonNode serviceAccountByUsername =
                objectMapper.readTree(
                        "{\"username\":\"service-account-commafeed\",\"email\":\"sa@example.com\"}");
        Assertions.assertTrue(mapper.isServiceAccount(serviceAccountByUsername));

        JsonNode serviceAccountByClientId =
                objectMapper.readTree(
                        "{\"username\":\"custom_sa\",\"serviceAccountClientId\":\"commafeed-service\"}");
        Assertions.assertTrue(mapper.isServiceAccount(serviceAccountByClientId));

        Assertions.assertFalse(mapper.isServiceAccount(null));
    }

    @Test
    void testToUserResponse() throws Exception {
        JsonNode userJson =
                objectMapper.readTree(
                        """
            {
                "id": "uuid-123",
                "username": "bob",
                "email": "bob@example.com",
                "firstName": "Robert",
                "lastName": "Smith",
                "enabled": true
            }
            """);

        KeycloakUserResponse response = mapper.toUserResponse(userJson);
        Assertions.assertNotNull(response);
        Assertions.assertEquals("uuid-123", response.getKeycloakId());
        Assertions.assertEquals("bob", response.getUsername());
        Assertions.assertEquals("bob@example.com", response.getEmail());
        Assertions.assertEquals("Robert", response.getFirstName());
        Assertions.assertEquals("Smith", response.getLastName());
        Assertions.assertTrue(response.isEnabled());
    }

    @Test
    void testToUserDetailResponse() {
        KeycloakUserResponse base =
                KeycloakUserResponse.builder()
                        .keycloakId("uuid-123")
                        .username("bob")
                        .email("bob@example.com")
                        .firstName("Robert")
                        .lastName("Smith")
                        .enabled(true)
                        .build();

        List<String> roles = List.of("admin", "editor");
        List<String> groups = List.of("Engineering");

        KeycloakUserDetailResponse detail = mapper.toUserDetailResponse(base, roles, groups);
        Assertions.assertNotNull(detail);
        Assertions.assertEquals("uuid-123", detail.getKeycloakId());
        Assertions.assertEquals("bob", detail.getUsername());
        Assertions.assertEquals(roles, detail.getRoles());
        Assertions.assertEquals(groups, detail.getGroups());
    }
}
