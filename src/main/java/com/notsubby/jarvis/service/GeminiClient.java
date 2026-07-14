package com.notsubby.jarvis.service;

import java.util.concurrent.CompletableFuture;

public interface GeminiClient {
    CompletableFuture<String> generateResponse(String prompt);
}
