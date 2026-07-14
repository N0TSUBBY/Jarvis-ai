package com.notsubby.jarvis.service.impl;

import com.notsubby.jarvis.model.AudioMetadataEvent;
import com.notsubby.jarvis.service.TtsService;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class MockTtsService implements TtsService {

    @Override
    public CompletableFuture<AudioMetadataEvent> synthesize(String sessionId, String turnId, String text) {
        long estimatedDuration = Math.max(300, text.length() * 35L);
        return CompletableFuture.completedFuture(new AudioMetadataEvent(
                sessionId,
                turnId,
                "mock-tts",
                "jarvis-neutral",
                "audio/wav",
                estimatedDuration
        ));
    }
}
