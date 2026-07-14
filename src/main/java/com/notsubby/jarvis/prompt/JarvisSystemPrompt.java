package com.notsubby.jarvis.prompt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JarvisSystemPrompt {

    private final String prompt;

    public JarvisSystemPrompt(@Value("classpath:prompts/jarvis-system-prompt.txt") Resource resource) {
        this.prompt = readPrompt(resource);
    }

    public String value() {
        return prompt;
    }

    private String readPrompt(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8).trim();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load system prompt", ex);
        }
    }
}
