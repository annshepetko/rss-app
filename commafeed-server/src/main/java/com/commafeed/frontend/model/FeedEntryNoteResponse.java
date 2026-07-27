package com.commafeed.frontend.model;

import com.commafeed.backend.model.FeedEntryNote;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.io.Serializable;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@SuppressWarnings("serial")
@Schema(description = "Feed Entry Note")
@Data
@RegisterForReflection
public class FeedEntryNoteResponse implements Serializable {

    @Schema(description = "note id", required = true)
    private Long id;

    @Schema(description = "entry id", required = true)
    private Long entryId;

    @Schema(description = "comment text", required = true)
    private String comment;

    @Schema(description = "rating between 1 and 5", required = true)
    private Integer rating;

    public static FeedEntryNoteResponse build(FeedEntryNote note) {
        FeedEntryNoteResponse model = new FeedEntryNoteResponse();
        model.setId(note.getId());
        model.setEntryId(note.getEntry() != null ? note.getEntry().getId() : null);
        model.setComment(note.getComment());
        model.setRating(note.getRating());
        return model;
    }
}
