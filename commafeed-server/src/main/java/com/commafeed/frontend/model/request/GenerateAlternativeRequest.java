package com.commafeed.frontend.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@SuppressWarnings("serial")
@Schema(description = "Generate Alternative Request")
@Data
public class GenerateAlternativeRequest implements Serializable {

    @Schema(description = "target field: 'title' or 'content'", required = true)
    @NotNull
    private Target target;

    @Schema(description = "free-text prompt instruction for the LLM", required = true)
    @NotEmpty
    private String prompt;
}
