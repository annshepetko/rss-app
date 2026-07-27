package com.commafeed.frontend.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@SuppressWarnings("serial")
@Schema(description = "Add Note Request")
@Data
public class AddNoteRequest implements Serializable {

    @Schema(description = "entry id", required = true)
    @NotNull
    private Long entryId;

    @Schema(description = "comment note text", required = true)
    @NotEmpty
    @Size(max = 1000)
    private String comment;

    @Schema(description = "rating between 1 and 5", required = true)
    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;
}
