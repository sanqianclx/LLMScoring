package com.llm.open.llmscoring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm.open.llmscoring.config.LlmProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleLlmClient {

    private final ObjectMapper objectMapper;
    private final LlmProperties properties;
    private final HttpClient httpClient;

    public OpenAiCompatibleLlmClient(ObjectMapper objectMapper, LlmProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, properties.getConnectTimeoutSeconds())))
                .build();
    }

    public ChatCompletionResult complete(String systemPrompt, String userPrompt) {
        try {
            String requestBody = objectMapper.writeValueAsString(buildRequestBody(systemPrompt, userPrompt));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(properties.getBaseUrl()) + "/chat/completions"))
                    .timeout(Duration.ofSeconds(Math.max(1, properties.getReadTimeoutSeconds())))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("HTTP " + response.statusCode() + " from LLM provider: " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.at("/choices/0/message/content").asText(null);
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("LLM response did not include choices[0].message.content");
            }
            String modelName = root.path("model").asText(properties.getModel());
            return new ChatCompletionResult(content, modelName, response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to serialize or parse LLM payload", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM request was interrupted", exception);
        }
    }

    private Map<String, Object> buildRequestBody(String systemPrompt, String userPrompt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getModel());
        payload.put("temperature", properties.getTemperature());
        payload.put("max_tokens", properties.getMaxCompletionTokens());
        payload.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        if (properties.isJsonMode()) {
            payload.put("response_format", Map.of("type", "json_object"));
        }
        return payload;
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public record ChatCompletionResult(
            String content,
            String model,
            String rawResponse
    ) {
    }
}
