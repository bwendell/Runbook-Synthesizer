package com.oracle.runbook.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EnrichedContext} record.
 */
class EnrichedContextTest {

	private Alert createTestAlert() {
		return new Alert("alert-123", "High CPU", "CPU exceeded threshold", AlertSeverity.WARNING, "oci-monitoring",
				Map.of(), Map.of(), Instant.now(), "{}");
	}

	private ResourceMetadata createTestMetadata() {
		return new ResourceMetadata("ocid1.instance.oc1..xxx", "web-01", "ocid1.compartment..xxx", "VM.Standard2.4",
				"AD-1", Map.of(), Map.of());
	}

	@Test
	@DisplayName("EnrichedContext construction with all components succeeds")
	void constructionWithAllComponentsSucceeds() {
		Alert alert = createTestAlert();
		ResourceMetadata resource = createTestMetadata();
		List<MetricSnapshot> metrics = List.of(new MetricSnapshot("CpuUtilization", "oci", 85.0, "%", Instant.now()));
		List<LogEntry> logs = List.of(new LogEntry("log-1", Instant.now(), "ERROR", "error msg", Map.of()));
		Map<String, Object> customProps = Map.of("gpuStatus", "healthy");

		EnrichedContext context = new EnrichedContext(alert, resource, metrics, logs, customProps);

		assertEquals(alert, context.alert());
		assertEquals(resource, context.resource());
		assertEquals(metrics, context.recentMetrics());
		assertEquals(logs, context.recentLogs());
		assertEquals(customProps, context.customProperties());
	}

	@Test
	@DisplayName("EnrichedContext throws NullPointerException for null alert")
	void throwsForNullAlert() {
		assertThrows(NullPointerException.class,
				() -> new EnrichedContext(null, createTestMetadata(), List.of(), List.of(), Map.of()));
	}

	@Test
	@DisplayName("EnrichedContext metrics list is immutable")
	void metricsListIsImmutable() {
		List<MetricSnapshot> mutableMetrics = new ArrayList<>();
		mutableMetrics.add(new MetricSnapshot("cpu", "ns", 50.0, "%", Instant.now()));

		EnrichedContext context = new EnrichedContext(createTestAlert(), createTestMetadata(), mutableMetrics,
				List.of(), Map.of());

		// Modifying original should not affect context
		mutableMetrics.add(new MetricSnapshot("mem", "ns", 60.0, "%", Instant.now()));
		assertEquals(1, context.recentMetrics().size());

		// Context's list should be unmodifiable
		assertThrows(UnsupportedOperationException.class, () -> context.recentMetrics().add(null));
	}

	@Test
	@DisplayName("EnrichedContext logs list is immutable")
	void logsListIsImmutable() {
		List<LogEntry> mutableLogs = new ArrayList<>();
		mutableLogs.add(new LogEntry("1", Instant.now(), "INFO", "msg", Map.of()));

		EnrichedContext context = new EnrichedContext(createTestAlert(), createTestMetadata(), List.of(), mutableLogs,
				Map.of());

		// Modifying original should not affect context
		mutableLogs.add(new LogEntry("2", Instant.now(), "ERROR", "msg2", Map.of()));
		assertEquals(1, context.recentLogs().size());

		// Context's list should be unmodifiable
		assertThrows(UnsupportedOperationException.class, () -> context.recentLogs().add(null));
	}

	@Test
	@DisplayName("EnrichedContext customProperties map is immutable")
	void customPropertiesMapIsImmutable() {
		Map<String, Object> mutableProps = new HashMap<>();
		mutableProps.put("key", "value");

		EnrichedContext context = new EnrichedContext(createTestAlert(), createTestMetadata(), List.of(), List.of(),
				mutableProps);

		// Modifying original should not affect context
		mutableProps.put("newKey", "newValue");
		assertFalse(context.customProperties().containsKey("newKey"));

		// Context's map should be unmodifiable
		assertThrows(UnsupportedOperationException.class, () -> context.customProperties().put("another", "value"));
	}
}
