package com.commafeed.frontend;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

@Data
@RegisterForReflection
public class OriginalContentDto {

    private String entryId;
    private String title;
    private String content;

    public OriginalContentDto(String entryId, String title, String content) {
        this.entryId = entryId;
        this.title = title;
        this.content = content;
    }
}
