package com.notsubby.jarvis.service;

import com.notsubby.jarvis.config.JarvisProperties;
import com.notsubby.jarvis.events.SessionEventPublisher;
import com.notsubby.jarvis.model.AssistantTextEvent;
import com.notsubby.jarvis.model.AudioMetadataEvent;
import com.notsubby.jarvis.model.StatusEvent;
import com.notsubby.jarvis.model.TranscriptEvent;
import com.notsubby.jarvis.prompt.JarvisSystemPrompt;
import com.notsubby.jarvis.prompt.PromptBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

class AudioSessionCoordinatorTest {

    @Test
    void coordinatorPublishesAssistantAndAudioEvents() {
        InMemoryPublisher publisher = new InMemoryPublisher();
        JarvisProperties props = new JarvisProperties();
        props.setMemoryWindow(3);
        Executor directExecutor = Runnable::run;

        AudioSessionCoordinator coordinator = new AudioSessionCoordinator(
                transcript -> CompletableFuture.completedFuture(transcript.trim()),
                new PromptBuilder(new JarvisSystemPrompt(new ByteArrayResource("system".getBytes()))),
                prompt -> CompletableFuture.completedFuture("Acknowledged."),
                (sessionId, turnId, text) -> CompletableFuture.completedFuture(new AudioMetadataEvent(sessionId, turnId, "mock", "voice", "audio/wav", 500)),
                publisher,
                directExecutor,
                props
        );

        coordinator.handleTranscript(new TranscriptEvent("s1", "hello"));

        assertThat(publisher.assistantTexts).hasSize(1);
        assertThat(publisher.assistantTexts.getFirst().text()).isEqualTo("Acknowledged.");
        assertThat(publisher.audioMetadata).hasSize(1);
        assertThat(publisher.statusEvents).isNotEmpty();
        assertThat(publisher.statusEvents.getLast().state()).isEqualTo("listening");
    }

    @Test
    void blankTranscriptPublishesErrorStatus() {
        InMemoryPublisher publisher = new InMemoryPublisher();
        AudioSessionCoordinator coordinator = new AudioSessionCoordinator(
                transcript -> CompletableFuture.completedFuture(transcript),
                new PromptBuilder(new JarvisSystemPrompt(new ByteArrayResource("system".getBytes()))),
                prompt -> CompletableFuture.completedFuture("ok"),
                (sessionId, turnId, text) -> CompletableFuture.completedFuture(new AudioMetadataEvent(sessionId, turnId, "mock", "voice", "audio/wav", 100)),
                publisher,
                Runnable::run,
                new JarvisProperties()
        );

        coordinator.handleTranscript(new TranscriptEvent("s1", " "));

        assertThat(publisher.statusEvents).hasSize(1);
        assertThat(publisher.statusEvents.getFirst().state()).isEqualTo("error");
    }

    private static class InMemoryPublisher implements SessionEventPublisher {
        private final List<AssistantTextEvent> assistantTexts = new ArrayList<>();
        private final List<AudioMetadataEvent> audioMetadata = new ArrayList<>();
        private final List<StatusEvent> statusEvents = new ArrayList<>();

        @Override
        public void publishAssistantText(AssistantTextEvent event) {
            assistantTexts.add(event);
        }

        @Override
        public void publishAudioMetadata(AudioMetadataEvent event) {
            audioMetadata.add(event);
        }

        @Override
        public void publishStatus(StatusEvent event) {
            statusEvents.add(event);
        }
    }
}
