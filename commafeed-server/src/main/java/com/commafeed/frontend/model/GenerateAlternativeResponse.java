package com.commafeed.frontend.model;

import com.commafeed.frontend.OriginalContentDto;
import com.commafeed.frontend.model.request.Target;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.io.Serializable;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@SuppressWarnings("serial")
@Schema(description = "Generate Alternative Response")
@Data
@RegisterForReflection
public class GenerateAlternativeResponse implements Serializable {

    private OriginalContentDto original;

    @Schema(description = "target field: 'title' or 'content'", required = true)
    private Target target;

    @Schema(description = "original prompt instruction", required = true)
    private String prompt;

    @Schema(description = "generated alternative output from the LLM", required = true)
    private String generatedAlternative;
}
