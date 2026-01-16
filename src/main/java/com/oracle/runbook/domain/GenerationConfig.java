package com.oracle.runbook.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * Configuration parameters for LLM text generation.
 *
 * @param temperature controls randomness (0.0 = deterministic, 1.0 = creative)
 * @param maxTokens maximum tokens in the generated response
 * @param modelOverride optional override for the default model
 */
public record GenerationConfig(
    double temperature,
    int maxTokens,
    Optional<String> modelOverride
) {
    /**
     * Compact constructor with validation.
     */
    public GenerationConfig {
        if (temperature < 0.0 || temperature > 1.0) {
            throw new IllegalArgumentException(
                "Temperature must be between 0.0 and 1.0, got: " + temperature
            );
        }
        if (maxTokens <= 0) {
            throw new IllegalArgumentException(
                "maxTokens must be greater than 0, got: " + maxTokens
            );
        }
        Objects.requireNonNull(modelOverride, "modelOverride cannot be null (use Optional.empty())");
    }
}
