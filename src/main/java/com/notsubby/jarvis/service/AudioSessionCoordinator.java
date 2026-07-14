package com.notsubby.jarvis.service;

import com.notsubby.jarvis.config.JarvisProperties;
import com.notsubby.jarvis.events.SessionEventPublisher;
import com.notsubby.jarvis.model.AssistantTextEvent;
import com.notsubby.jarvis.model.AudioMetadataEvent;
import com.notsubby.jarvis.model.StatusEvent;
import com.notsubby.jarvis.model.TranscriptEvent;
import com.notsubby.jarvis.prompt.PromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AudioSessionCoordinator {

    private static final Logger log = LoggerFactory.getLogger(AudioSessionCoordinator.class);

    private final SttService sttService;
    private final PromptBuilder promptBuilder;
    private final GeminiClient geminiClient;
    private final TtsService ttsService;
    private final SessionEventPublisher publisher;
    private final Executor assistantExecutor;
    private final JarvisProperties properties;
    private final Map<String, Deque<String>> sessionMemory = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> sessionTurns = new ConcurrentHashMap<>();

    public AudioSessionCoordinator(
            SttService sttService,
            PromptBuilder promptBuilder,
            GeminiClient geminiClient,
            TtsService ttsService,
            SessionEventPublisher publisher,
            Executor assistantExecutor,
            JarvisProperties properties
    ) {
        this.sttService = sttService;
        this.promptBuilder = promptBuilder;
        this.geminiClient = geminiClient;
        this.ttsService = ttsService;
        this.publisher = publisher;
        this.assistantExecutor = assistantExecutor;
        this.properties = properties;
    }

    public void handleTranscript(TranscriptEvent event) {
        if (event.text() == null || event.text().isBlank()) {
            publishStatus(event.sessionId(), null, "error", 0, "Transcript text is blank");
            return;
        }

        String sessionId = event.sessionId();
        String turnId = "turn-" + sessionTurns.computeIfAbsent(sessionId, unused -> new AtomicLong()).incrementAndGet();
        long startedAt = System.nanoTime();
        publishStatus(sessionId, turnId, "thinking", 0, "Processing transcript");

        CompletableFuture.supplyAsync(event::text, assistantExecutor)
                .thenCompose(sttService::normalizeTranscript)
                .thenCompose(transcript -> {
                    addMemory(sessionId, "User: " + transcript);
                    String prompt = promptBuilder.buildPrompt(sessionId, transcript, memorySnapshot(sessionId));
                    return geminiClient.generateResponse(prompt);
                })
                .thenCompose(reply -> {
                    addMemory(sessionId, "Assistant: " + reply);
                    publisher.publishAssistantText(new AssistantTextEvent(sessionId, turnId, reply));
                    publishStatus(sessionId, turnId, "speaking", elapsedMs(startedAt), "Assistant response ready");
                    return ttsService.synthesize(sessionId, turnId, reply);
                })
                .thenAccept(audio -> {
                    publisher.publishAudioMetadata(audio);
                    publishStatus(sessionId, turnId, "listening", elapsedMs(startedAt), "Awaiting next command");
                    log.atInfo()
                            .addKeyValue("sessionId", sessionId)
                            .addKeyValue("turnId", turnId)
                            .addKeyValue("latencyMs", elapsedMs(startedAt))
                            .log("Voice turn completed");
                })
                .exceptionally(ex -> {
                    log.atError()
                            .setCause(ex)
                            .addKeyValue("sessionId", sessionId)
                            .addKeyValue("turnId", turnId)
                            .log("Voice turn failed");
                    publishStatus(sessionId, turnId, "error", elapsedMs(startedAt), ex.getMessage());
                    return null;
                });
    }

    private void addMemory(String sessionId, String value) {
        Deque<String> memory = sessionMemory.computeIfAbsent(sessionId, ignored -> new ArrayDeque<>());
        synchronized (memory) {
            memory.addLast(value);
            while (memory.size() > properties.getMemoryWindow()) {
                memory.removeFirst();
            }
        }
    }

    private List<String> memorySnapshot(String sessionId) {
        Deque<String> memory = sessionMemory.computeIfAbsent(sessionId, ignored -> new ArrayDeque<>());
        synchronized (memory) {
            return new ArrayList<>(memory);
        }
    }

    private void publishStatus(String sessionId, String turnId, String state, long latencyMs, String message) {
        publisher.publishStatus(new StatusEvent(sessionId, turnId, state, latencyMs, message, Instant.now()));
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
