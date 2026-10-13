package com.commafeed.integration.rest;

import com.commafeed.TestConstants;
import com.commafeed.frontend.model.request.AdminSaveUserRequest;
import com.commafeed.integration.BaseIT;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class KeycloakAdminIT extends BaseIT {

    private static final String NON_ADMIN_USER = "regularuser";
    private static final String NON_ADMIN_PASSWORD = "Password123!";

    @BeforeEach
    void setup() {
        // Initial setup creates the first user as admin
        initialSetup(TestConstants.ADMIN_USERNAME, TestConstants.ADMIN_PASSWORD);

        // Authenticate as Admin to create a non-admin user
        RestAssured.authentication =
                RestAssured.preemptive()
                        .basic(TestConstants.ADMIN_USERNAME, TestConstants.ADMIN_PASSWORD);

        AdminSaveUserRequest user = new AdminSaveUserRequest();
        user.setName(NON_ADMIN_USER);
        user.setPassword(NON_ADMIN_PASSWORD);
        user.setEmail(NON_ADMIN_USER + "@commafeed.com");
        user.setEnabled(true);
        user.setAdmin(false);

        RestAssured.given()
                .body(user)
                .contentType(ContentType.JSON)
                .post("rest/admin/user/save")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @AfterEach
    void cleanup() {
        RestAssured.reset();
    }

    @Test
    void nonAdminRequestReturns403Forbidden() {
        // Authenticate as a regular non-admin user
        RestAssured.authentication =
                RestAssured.preemptive().basic(NON_ADMIN_USER, NON_ADMIN_PASSWORD);

        RestAssured.given()
                .get("rest/admin/directory/users")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        RestAssured.given()
                .get("rest/admin/directory/users/dummy-uuid")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }
}
