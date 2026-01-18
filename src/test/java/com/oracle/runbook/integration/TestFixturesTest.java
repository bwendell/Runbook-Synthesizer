package com.oracle.runbook.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.domain.*;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for TestFixtures utility class. */
@DisplayName("TestFixtures")
class TestFixturesTest {

  @Nested
  @DisplayName("loadJson")
  class LoadJsonTests {

    @Test
    @DisplayName("returns valid JsonObject for existing fixture")
    void loadJson_returnsValidJsonObject() {
      JsonObject json = TestFixtures.loadJson("alerts/high-memory-alert.json");

      assertThat(json).isNotNull();
      assertThat(json.containsKey("title")).isTrue();
    }

    @Test
    @DisplayName("throws IllegalArgumentException for missing file")
    void loadJson_throwsForMissingFile() {
      assertThatThrownBy(() -> TestFixtures.loadJson("nonexistent.json"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("loads Slack webhook payload fixture")
    void loadJson_loadsSlackPayload() {
      JsonObject json = TestFixtures.loadJson("webhooks/slack-payload.json");

      assertThat(json).isNotNull();
      assertThat(json.containsKey("blocks")).isTrue();
    }

    @Test
    @DisplayName("loads PagerDuty webhook payload fixture")
    void loadJson_loadsPagerDutyPayload() {
      JsonObject json = TestFixtures.loadJson("webhooks/pagerduty-payload.json");

      assertThat(json).isNotNull();
      assertThat(json.containsKey("routing_key")).isTrue();
    }
  }

  @Nested
  @DisplayName("loadAs")
  class LoadAsTests {

    @Test
    @DisplayName("deserializes Alert from fixture")
    void loadAs_deserializesToExpectedType() {
      Alert alert = TestFixtures.loadAs("alerts/high-memory-alert.json", Alert.class);

      assertThat(alert).isNotNull();
      assertThat(alert.title()).isNotBlank();
    }

    @Test
    @DisplayName("deserializes EnrichedContext from fixture")
    void loadAs_loadsEnrichedContext() {
      EnrichedContext context =
          TestFixtures.loadAs("contexts/enriched-context-memory.json", EnrichedContext.class);

      assertThat(context).isNotNull();
      assertThat(context.alert()).isNotNull();
      assertThat(context.alert().id()).isEqualTo("alert-001");
      assertThat(context.alert().severity()).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    @DisplayName("deserializes DynamicChecklist from fixture")
    void loadAs_loadsDynamicChecklist() {
      DynamicChecklist checklist =
          TestFixtures.loadAs("checklists/sample-checklist.json", DynamicChecklist.class);

      assertThat(checklist).isNotNull();
      assertThat(checklist.alertId()).isEqualTo("alert-123");
      assertThat(checklist.steps()).isNotEmpty();
      assertThat(checklist.steps().getFirst().priority()).isEqualTo(StepPriority.HIGH);
    }
  }
}
