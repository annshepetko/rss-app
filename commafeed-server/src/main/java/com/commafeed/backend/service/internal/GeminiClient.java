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
public class GeminiClient implements LLMClient {

    private final CommaFeedConfiguration config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Override
    public String generateAlternative(String prompt, String originalText) {
        String apiKey = getApiKey();
        if (StringUtils.isBlank(apiKey)) {
            throw new LLMException(
                    "Gemini API key is not configured. Please set COMMAFEED_GEMINI_API_KEY or GEMINI_API_KEY environment variable.");
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
            ArrayNode contents = requestJson.putArray("contents");
            ObjectNode contentObj = contents.addObject();
            ArrayNode parts = contentObj.putArray("parts");
            ObjectNode partObj = parts.addObject();
            partObj.put("text", combinedText);

            String requestBody = objectMapper.writeValueAsString(requestJson);
            String endpointUrl = config.geminiApiUrl() + "?key=" + apiKey;

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(endpointUrl))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                            .timeout(Duration.ofSeconds(30))
                            .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error(
                        "Gemini API error status: {}, body: {}",
                        response.statusCode(),
                        response.body());
                throw new LLMException(
                        "Gemini API call failed with status code: " + response.statusCode());
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode textNode = rootNode.at("/candidates/0/content/parts/0/text");

            if (textNode.isMissingNode() || StringUtils.isBlank(textNode.asText())) {
                throw new LLMException("Gemini API returned an empty or invalid response.");
            }

            return textNode.asText().trim();

        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate alternative content using Gemini API", e);
            throw new LLMException(
                    "Error communicating with Gemini LLM service: " + e.getMessage());
        }
    }

    private String getApiKey() {
        Optional<String> configuredKey = config.geminiApiKey();
        if (configuredKey.isPresent() && StringUtils.isNotBlank(configuredKey.get())) {
            return configuredKey.get();
        }

        String envKey = System.getenv("COMMAFEED_GEMINI_API_KEY");
        if (StringUtils.isNotBlank(envKey)) {
            return envKey;
        }

        return System.getenv("GEMINI_API_KEY");
    }
}
