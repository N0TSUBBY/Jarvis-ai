package com.notsubby.jarvis.service;

import java.util.concurrent.CompletableFuture;

public interface SttService {
    CompletableFuture<String> normalizeTranscript(String transcript);
}
