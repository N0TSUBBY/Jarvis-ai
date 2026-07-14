package com.notsubby.jarvis.prompt;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {

    private final JarvisSystemPrompt systemPrompt;

    public PromptBuilder(JarvisSystemPrompt systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String buildPrompt(String sessionId, String userTranscript, List<String> memory) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("System Instruction:\n")
                .append(systemPrompt.value())
                .append("\n\nSession: ").append(sessionId)
                .append("\nShort-term memory:\n");

        if (memory.isEmpty()) {
            prompt.append("- (empty)\n");
        } else {
            memory.forEach(item -> prompt.append("- ").append(item).append("\n"));
        }

        prompt.append("\nUser transcript:\n")
                .append(userTranscript)
                .append("\n\nRespond with concise, voice-friendly text.");

        return prompt.toString();
    }
}
