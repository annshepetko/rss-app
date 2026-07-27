package com.commafeed.frontend.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Target {
    @JsonProperty("title")
    TITLE,

    @JsonProperty("content")
    CONTENT
}
