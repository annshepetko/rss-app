package com.commafeed.frontend.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@SuppressWarnings("serial")
@Schema(description = "Keycloak user information for list view")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class KeycloakUserResponse implements Serializable {

    @Schema(description = "Keycloak UUID", required = true)
    private String keycloakId;

    @Schema(description = "Username", required = true)
    private String username;

    @Schema(description = "Email address")
    private String email;

    @Schema(description = "First name")
    private String firstName;

    @Schema(description = "Last name")
    private String lastName;

    @Schema(description = "Enabled status", required = true)
    private boolean enabled;
}
