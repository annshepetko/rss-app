package com.commafeed.backend.service.internal;

import com.commafeed.CommaFeedConfiguration;
import com.commafeed.backend.service.LLMClient;
import com.commafeed.exceptions.LLMException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class GroqClient implements LLMClient {

    private final CommaFeedConfiguration config;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Override
    public String generateAlternative(String prompt, String originalText) {

        String apiKey = getApiKey();

        if (StringUtils.isBlank(apiKey)) {
            throw new LLMException("Groq API key is not configured.");
        }

        String combinedText =
                "Rewrite the following text.\n\n"
                        + "Instruction:\n"
                        + prompt
                        + "\n\n"
                        + "Original text:\n"
                        + StringUtils.defaultString(originalText)
                        + "\n\n"
                        + "Return only the rewritten text.";

        try {

            ObjectNode requestJson = objectMapper.createObjectNode();
            requestJson.put("model", "llama-3.3-70b-versatile");
            requestJson.put("temperature", 0.7);

            ArrayNode messages = requestJson.putArray("messages");

            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", "You rewrite text. Return only the rewritten text.");

            ObjectNode user = messages.addObject();
            user.put("role", "user");
            user.put("content", combinedText);

            String requestBody = objectMapper.writeValueAsString(requestJson);

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(config.groqApiUrl()))
                            .header("Authorization", "Bearer " + apiKey)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                            .timeout(Duration.ofSeconds(30))
                            .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error(
                        "Groq API error status: {}, body: {}",
                        response.statusCode(),
                        response.body());

                throw new LLMException(
                        "Groq API call failed (" + response.statusCode() + "): " + response.body());
            }

            JsonNode rootNode = objectMapper.readTree(response.body());

            JsonNode textNode = rootNode.path("choices").get(0).path("message").path("content");

            if (textNode.isMissingNode() || StringUtils.isBlank(textNode.asText())) {
                throw new LLMException("Groq API returned an empty response.");
            }

            return textNode.asText().trim();

        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call Groq API", e);
            throw new LLMException("Error communicating with Groq API: " + e.getMessage());
        }
    }

    private String getApiKey() {
        Optional<String> configuredKey = config.groqApiKey();

        if (configuredKey.isPresent() && StringUtils.isNotBlank(configuredKey.get())) {
            return configuredKey.get();
        }

        return System.getenv("GROQ_API_KEY");
    }
}
