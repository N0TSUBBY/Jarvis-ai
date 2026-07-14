package com.notsubby.jarvis.model;

public record AudioMetadataEvent(
        String sessionId,
        String turnId,
        String provider,
        String voice,
        String audioFormat,
        long durationMs
) {
}
