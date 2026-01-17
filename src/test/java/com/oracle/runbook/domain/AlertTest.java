package com.oracle.runbook.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Alert} record.
 */
class AlertTest {

	@Test
	@DisplayName("Alert construction with all required fields succeeds")
	void constructionWithAllFieldsSucceeds() {
		Instant now = Instant.now();
		Map<String, String> dimensions = Map.of("compartmentId", "ocid1.compartment.oc1..xxx");
		Map<String, String> labels = Map.of("env", "production");

		Alert alert = new Alert("alert-123", "High CPU Usage", "CPU usage exceeded 90% threshold",
				AlertSeverity.CRITICAL, "oci-monitoring", dimensions, labels, now, "{\"raw\": \"payload\"}");

		assertEquals("alert-123", alert.id());
		assertEquals("High CPU Usage", alert.title());
		assertEquals("CPU usage exceeded 90% threshold", alert.message());
		assertEquals(AlertSeverity.CRITICAL, alert.severity());
		assertEquals("oci-monitoring", alert.sourceService());
		assertEquals(dimensions, alert.dimensions());
		assertEquals(labels, alert.labels());
		assertEquals(now, alert.timestamp());
		assertEquals("{\"raw\": \"payload\"}", alert.rawPayload());
	}

	@Test
	@DisplayName("Alert throws NullPointerException for null id")
	void throwsForNullId() {
		assertThrows(NullPointerException.class, () -> new Alert(null, "Title", "Message", AlertSeverity.WARNING,
				"source", Map.of(), Map.of(), Instant.now(), "{}"));
	}

	@Test
	@DisplayName("Alert throws NullPointerException for null title")
	void throwsForNullTitle() {
		assertThrows(NullPointerException.class, () -> new Alert("id", null, "Message", AlertSeverity.WARNING, "source",
				Map.of(), Map.of(), Instant.now(), "{}"));
	}

	@Test
	@DisplayName("Alert dimensions map is immutable")
	void dimensionsMapIsImmutable() {
		Map<String, String> mutableDimensions = new HashMap<>();
		mutableDimensions.put("key", "value");

		Alert alert = new Alert("id", "Title", "Message", AlertSeverity.INFO, "source", mutableDimensions, Map.of(),
				Instant.now(), "{}");

		// Modifying original should not affect alert
		mutableDimensions.put("newKey", "newValue");
		assertFalse(alert.dimensions().containsKey("newKey"));

		// Alert's dimensions should be unmodifiable
		assertThrows(UnsupportedOperationException.class, () -> alert.dimensions().put("another", "value"));
	}

	@Test
	@DisplayName("Alert labels map is immutable")
	void labelsMapIsImmutable() {
		Map<String, String> mutableLabels = new HashMap<>();
		mutableLabels.put("env", "prod");

		Alert alert = new Alert("id", "Title", "Message", AlertSeverity.INFO, "source", Map.of(), mutableLabels,
				Instant.now(), "{}");

		// Modifying original should not affect alert
		mutableLabels.put("newKey", "newValue");
		assertFalse(alert.labels().containsKey("newKey"));

		// Alert's labels should be unmodifiable
		assertThrows(UnsupportedOperationException.class, () -> alert.labels().put("another", "value"));
	}
}
