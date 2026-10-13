package com.commafeed.frontend.resource;

import com.commafeed.backend.service.KeycloakService;
import com.commafeed.frontend.model.KeycloakUserDetailResponse;
import com.commafeed.frontend.model.KeycloakUserResponse;
import com.commafeed.security.Roles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/rest/admin/directory")
@RolesAllowed(Roles.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Singleton
@Tag(name = "Admin Directory")
public class KeycloakAdminREST {

    private static final int DEFAULT_FIRST = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;

    private final KeycloakService keycloakService;

    @Path("/users")
    @GET
    @Operation(
            summary = "List Keycloak users",
            description = "Fetch paginated regular Keycloak users excluding service accounts")
    public List<KeycloakUserResponse> getUsers(
            @Parameter(description = "Search term for filtering users") @QueryParam("search")
                    String search,
            @Parameter(description = "First result index (0-based)")
                    @QueryParam("first")
                    @DefaultValue("0")
                    Integer first,
            @Parameter(description = "Maximum results per page (1 to 100)")
                    @QueryParam("max")
                    @DefaultValue("20")
                    Integer max) {
        int safeFirst = sanitizeFirst(first);
        int safeMax = sanitizeMax(max);
        return keycloakService.getUsers(search, safeFirst, safeMax);
    }

    @Path("/users/{keycloakId}")
    @GET
    @Operation(
            summary = "Get single Keycloak user details",
            description = "Fetch single user details including realm roles and groups")
    public KeycloakUserDetailResponse getUserDetails(
            @Parameter(description = "Keycloak UUID", required = true) @PathParam("keycloakId")
                    String keycloakId) {
        return keycloakService.getUserDetails(keycloakId);
    }

    private int sanitizeFirst(Integer first) {
        if (first == null || first < DEFAULT_FIRST) {
            return DEFAULT_FIRST;
        }
        return first;
    }

    private int sanitizeMax(Integer max) {
        if (max == null || max < MIN_PAGE_SIZE || max > MAX_PAGE_SIZE) {
            return DEFAULT_PAGE_SIZE;
        }
        return max;
    }
}
