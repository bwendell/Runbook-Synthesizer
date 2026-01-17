package com.oracle.runbook.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GenerationConfig} record.
 */
class GenerationConfigTest {

	@Test
	@DisplayName("GenerationConfig construction with valid values succeeds")
	void constructionWithValidValuesSucceeds() {
		GenerationConfig config = new GenerationConfig(0.7, 2048, Optional.of("gpt-4-turbo"));

		assertEquals(0.7, config.temperature());
		assertEquals(2048, config.maxTokens());
		assertEquals(Optional.of("gpt-4-turbo"), config.modelOverride());
	}

	@Test
	@DisplayName("GenerationConfig allows empty modelOverride")
	void allowsEmptyModelOverride() {
		GenerationConfig config = new GenerationConfig(0.5, 1024, Optional.empty());

		assertEquals(Optional.empty(), config.modelOverride());
	}

	@ParameterizedTest
	@DisplayName("GenerationConfig throws for temperature outside 0.0-1.0 range")
	@ValueSource(doubles = {-0.1, -1.0, 1.1, 2.0, 100.0})
	void throwsForInvalidTemperature(double temperature) {
		assertThrows(IllegalArgumentException.class, () -> new GenerationConfig(temperature, 1024, Optional.empty()));
	}

	@Test
	@DisplayName("GenerationConfig accepts boundary temperature values")
	void acceptsBoundaryTemperatureValues() {
		assertDoesNotThrow(() -> new GenerationConfig(0.0, 1024, Optional.empty()));
		assertDoesNotThrow(() -> new GenerationConfig(1.0, 1024, Optional.empty()));
		assertDoesNotThrow(() -> new GenerationConfig(0.5, 1024, Optional.empty()));
	}

	@ParameterizedTest
	@DisplayName("GenerationConfig throws for maxTokens <= 0")
	@ValueSource(ints = {0, -1, -100})
	void throwsForInvalidMaxTokens(int maxTokens) {
		assertThrows(IllegalArgumentException.class, () -> new GenerationConfig(0.5, maxTokens, Optional.empty()));
	}

	@Test
	@DisplayName("GenerationConfig accepts valid maxTokens")
	void acceptsValidMaxTokens() {
		assertDoesNotThrow(() -> new GenerationConfig(0.5, 1, Optional.empty()));
		assertDoesNotThrow(() -> new GenerationConfig(0.5, 4096, Optional.empty()));
		assertDoesNotThrow(() -> new GenerationConfig(0.5, 100000, Optional.empty()));
	}
}
