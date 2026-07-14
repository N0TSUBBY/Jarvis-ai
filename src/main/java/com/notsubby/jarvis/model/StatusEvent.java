package com.notsubby.jarvis.model;

import java.time.Instant;

public record StatusEvent(
        String sessionId,
        String turnId,
        String state,
        long latencyMs,
        String message,
        Instant timestamp
) {
}
