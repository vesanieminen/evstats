package com.vesanieminen.views.crossword.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HighlightPosition {
    public final int inputIndex;
    public final int startIndex;
    public final int endIndex;

    @JsonCreator
    public HighlightPosition(@JsonProperty("inputIndex") int inputIndex,
                             @JsonProperty("startIndex") int startIndex,
                             @JsonProperty("endIndex") int endIndex) {
        this.inputIndex = inputIndex;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }
}
