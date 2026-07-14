package com.notsubby.jarvis.model;

public record AssistantTextEvent(
        String sessionId,
        String turnId,
        String text
) {
}
