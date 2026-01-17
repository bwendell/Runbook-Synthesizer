package com.oracle.runbook.enrichment;

import com.oracle.runbook.domain.MetricSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the MetricsSourceAdapter interface contract.
 */
class MetricsSourceAdapterTest {

	@Test
	@DisplayName("Mock implementation can be created and returns expected source type")
	void sourceType_returnsExpectedIdentifier() {
		// Arrange
		MetricsSourceAdapter adapter = new TestMetricsSourceAdapter("oci-monitoring");

		// Act
		String sourceType = adapter.sourceType();

		// Assert
		assertEquals("oci-monitoring", sourceType);
	}

	@Test
	@DisplayName("fetchMetrics accepts resourceId and lookback, returns CompletableFuture<List<MetricSnapshot>>")
	void fetchMetrics_acceptsResourceIdAndLookback_returnsCompletableFuture() throws Exception {
		// Arrange
		MetricsSourceAdapter adapter = new TestMetricsSourceAdapter("prometheus");
		String resourceId = "ocid1.instance.oc1..example";
		Duration lookback = Duration.ofMinutes(15);

		// Act
		CompletableFuture<List<MetricSnapshot>> future = adapter.fetchMetrics(resourceId, lookback);
		List<MetricSnapshot> metrics = future.get();

		// Assert
		assertNotNull(future);
		assertNotNull(metrics);
		assertEquals(1, metrics.size());
		assertEquals("CpuUtilization", metrics.get(0).metricName());
	}

	/**
	 * Test implementation of MetricsSourceAdapter for verifying interface contract.
	 */
	private static class TestMetricsSourceAdapter implements MetricsSourceAdapter {
		private final String sourceType;

		TestMetricsSourceAdapter(String sourceType) {
			this.sourceType = sourceType;
		}

		@Override
		public String sourceType() {
			return sourceType;
		}

		@Override
		public CompletableFuture<List<MetricSnapshot>> fetchMetrics(String resourceId, Duration lookback) {
			MetricSnapshot snapshot = new MetricSnapshot("CpuUtilization", "oci_computeagent", 45.5, "percent",
					Instant.now());
			return CompletableFuture.completedFuture(List.of(snapshot));
		}
	}
}
