package com.notsubby.jarvis.service.impl;

import com.notsubby.jarvis.config.JarvisProperties;
import com.notsubby.jarvis.service.GeminiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class GeminiApiClient implements GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiApiClient.class);

    private final JarvisProperties properties;
    private final WebClient webClient;

    public GeminiApiClient(JarvisProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder
                .baseUrl(properties.getGemini().getBaseUrl())
                .build();
    }

    @Override
    public CompletableFuture<String> generateResponse(String prompt) {
        if (properties.getGemini().getApiKey() == null || properties.getGemini().getApiKey().isBlank()) {
            return CompletableFuture.completedFuture("Gemini key is missing. Set GEMINI_API_KEY to enable live responses.");
        }

        String model = properties.getGemini().getModel();
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                ))
        );

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:generateContent")
                        .queryParam("key", properties.getGemini().getApiKey())
                        .build(model))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(error -> Mono.error(new IllegalStateException("Gemini request failed: " + error))))
                .bodyToMono(Map.class)
                .map(this::extractText)
                .doOnError(error -> log.error("Gemini request failed", error))
                .toFuture();
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> payload) {
        Object candidates = payload.get("candidates");
        if (candidates instanceof List<?> candidateList && !candidateList.isEmpty()) {
            Object first = candidateList.getFirst();
            if (first instanceof Map<?, ?> candidateMap) {
                Object content = candidateMap.get("content");
                if (content instanceof Map<?, ?> contentMap) {
                    Object parts = contentMap.get("parts");
                    if (parts instanceof List<?> partsList && !partsList.isEmpty()) {
                        Object part = partsList.getFirst();
                        if (part instanceof Map<?, ?> partMap && partMap.get("text") instanceof String text) {
                            return text.trim();
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("Gemini response did not include assistant text");
    }
}
