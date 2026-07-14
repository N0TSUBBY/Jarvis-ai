package com.notsubby.jarvis.prompt;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    @Test
    void buildPromptIncludesSystemInstructionMemoryAndTranscript() {
        PromptBuilder promptBuilder = new PromptBuilder(new JarvisSystemPrompt(new ByteArrayResource("system-jarvis".getBytes())));

        String result = promptBuilder.buildPrompt("session-1", "Open diagnostics", List.of("User: Hi", "Assistant: Hello"));

        assertThat(result).contains("System Instruction:\nsystem-jarvis");
        assertThat(result).contains("Session: session-1");
        assertThat(result).contains("- User: Hi");
        assertThat(result).contains("User transcript:\nOpen diagnostics");
    }
}
