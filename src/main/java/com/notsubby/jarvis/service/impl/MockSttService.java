package com.notsubby.jarvis.service.impl;

import com.notsubby.jarvis.service.SttService;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class MockSttService implements SttService {

    @Override
    public CompletableFuture<String> normalizeTranscript(String transcript) {
        return CompletableFuture.completedFuture(transcript == null ? "" : transcript.trim());
    }
}
