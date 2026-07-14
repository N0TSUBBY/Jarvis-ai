package com.notsubby.jarvis.service;

import com.notsubby.jarvis.model.AudioMetadataEvent;

import java.util.concurrent.CompletableFuture;

public interface TtsService {
    CompletableFuture<AudioMetadataEvent> synthesize(String sessionId, String turnId, String text);
}
