package com.healthdekho.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Spring AI implementation backed by Amazon Bedrock (Converse API).
 * Model, region and credentials come from configuration and the AWS default credential chain.
 */
@Component
public class SpringAiRecommendationClient implements RecommendationAiClient {

    private static final Logger log = LoggerFactory.getLogger(SpringAiRecommendationClient.class);
    private static final int MAX_GOALS_LENGTH = 500;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final String template;

    public SpringAiRecommendationClient(ObjectProvider<ChatClient.Builder> chatClientBuilders,
                                        ObjectMapper objectMapper,
                                        @Value("classpath:prompts/recommendation-v1.txt") Resource promptTemplate) {
        ChatClient.Builder builder = chatClientBuilders.getIfAvailable();
        this.chatClient = builder == null ? null : builder.build();
        this.objectMapper = objectMapper;
        try {
            this.template = promptTemplate.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Prompt template prompts/recommendation-v1.txt is missing", ex);
        }
    }

    @Override
    public Optional<RecommendationAiResponse> suggest(RecommendationPrompt prompt) {
        if (chatClient == null) {
            log.warn("No AI chat model is configured; recommendations will use the rule-based fallback");
            return Optional.empty();
        }
        String reply = chatClient.prompt()
                .user(render(prompt))
                .call()
                .content();
        return parse(reply);
    }

    String render(RecommendationPrompt prompt) {
        return template
                .replace("{{age}}", String.valueOf(prompt.age()))
                .replace("{{preferredTerm}}", prompt.preferredTermYears() == null
                        ? "no preference" : prompt.preferredTermYears() + " years")
                .replace("{{preferredType}}", prompt.preferredPolicyType() == null
                        ? "no preference" : prompt.preferredPolicyType().name())
                .replace("{{maxItems}}", String.valueOf(prompt.maxItems()))
                .replace("{{products}}", toJson(prompt))
                // Customer text goes in last, cleaned, so it can't inject template markers.
                .replace("{{goals}}", clean(prompt.insuranceGoals()));
    }

    Optional<RecommendationAiResponse> parse(String reply) {
        if (reply == null || reply.isBlank()) {
            return Optional.empty();
        }
        // Models sometimes wrap JSON in ```json fences or add a sentence: keep only the outer JSON object.
        int start = reply.indexOf('{');
        int end = reply.lastIndexOf('}');
        if (start < 0 || end <= start) {
            log.warn("AI reply had no JSON object");
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(reply.substring(start, end + 1), RecommendationAiResponse.class));
        } catch (JsonProcessingException ex) {
            log.warn("AI reply was not valid JSON: {}", ex.getOriginalMessage());
            return Optional.empty();
        }
    }

    private String toJson(RecommendationPrompt prompt) {
        try {
            return objectMapper.writeValueAsString(prompt.candidates());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Couldn't write the product list for the AI prompt", ex);
        }
    }

    private static String clean(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.replace("{{", "").replace("}}", "").replaceAll("\\s+", " ").trim();
        return cleaned.length() > MAX_GOALS_LENGTH ? cleaned.substring(0, MAX_GOALS_LENGTH) : cleaned;
    }
}
