package com.notsubby.jarvis.model;

import jakarta.validation.constraints.NotBlank;

public record TranscriptEvent(
        @NotBlank String sessionId,
        @NotBlank String text
) {
}
