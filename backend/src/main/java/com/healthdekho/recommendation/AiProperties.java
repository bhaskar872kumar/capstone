package com.healthdekho.recommendation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** app.ai.*: model metadata saved with each request, prompt version, AI timeout and result size. */
@ConfigurationProperties("app.ai")
public record AiProperties(
        @DefaultValue("unknown-model") String modelName,
        @DefaultValue("unknown") String modelVersion,
        @DefaultValue("rec-v1") String promptVersion,
        @DefaultValue("15s") Duration timeout,
        @DefaultValue("3") int maxItems) {
}
