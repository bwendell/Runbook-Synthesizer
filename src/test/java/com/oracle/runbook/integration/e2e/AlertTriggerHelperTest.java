package com.oracle.runbook.integration.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AlertTriggerHelper}.
 *
 * <p>Tests cover factory methods for creating alarm payloads and SNS envelope handling.
 */
class AlertTriggerHelperTest {

  @Nested
  @DisplayName("createCloudWatchAlarm")
  class CreateCloudWatchAlarmTests {

    @Test
    @DisplayName("Should create valid CloudWatch alarm with specified parameters")
    void shouldCreateValidCloudWatchAlarm() {
      // When
      String alarm =
          AlertTriggerHelper.createCloudWatchAlarm("TestHighMemory", "MemoryUtilization", 90.0);

      // Then - should be valid JSON
      JsonObject json = parseJson(alarm);
      assertThat(json).isNotNull();

      // Then - verify SNS envelope structure
      assertThat(json.getString("Type")).isEqualTo("Notification");
      assertThat(json.containsKey("MessageId")).isTrue();
      assertThat(json.containsKey("Message")).isTrue();

      // Then - verify nested CloudWatch alarm
      JsonObject alarmJson = parseJson(json.getString("Message"));
      assertThat(alarmJson.getString("AlarmName")).isEqualTo("TestHighMemory");
      assertThat(alarmJson.getString("NewStateValue")).isEqualTo("ALARM");

      // Then - verify trigger contains metric info
      JsonObject trigger = alarmJson.getJsonObject("Trigger");
      assertThat(trigger.getString("MetricName")).isEqualTo("MemoryUtilization");
      assertThat(trigger.getJsonNumber("Threshold").doubleValue()).isEqualTo(90.0);
    }

    @Test
    @DisplayName("Should throw for null alarm name")
    void shouldThrowForNullAlarmName() {
      assertThatThrownBy(
              () -> AlertTriggerHelper.createCloudWatchAlarm(null, "CPUUtilization", 80.0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("alarmName");
    }

    @Test
    @DisplayName("Should throw for empty alarm name")
    void shouldThrowForEmptyAlarmName() {
      assertThatThrownBy(() -> AlertTriggerHelper.createCloudWatchAlarm("", "CPUUtilization", 80.0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("alarmName");
    }

    @Test
    @DisplayName("Should include dimensions in trigger")
    void shouldIncludeDimensionsInTrigger() {
      // When
      String alarm = AlertTriggerHelper.createCloudWatchAlarm("TestAlarm", "CPUUtilization", 85.0);

      // Then
      JsonObject json = parseJson(alarm);
      JsonObject alarmJson = parseJson(json.getString("Message"));
      JsonObject trigger = alarmJson.getJsonObject("Trigger");

      assertThat(trigger.getJsonArray("Dimensions")).isNotEmpty();
      assertThat(trigger.getJsonArray("Dimensions").getJsonObject(0).getString("name"))
          .isEqualTo("InstanceId");
    }
  }

  @Nested
  @DisplayName("createOciMonitoringAlarm")
  class CreateOciMonitoringAlarmTests {

    @Test
    @DisplayName("Should create valid OCI Monitoring alarm with specified parameters")
    void shouldCreateValidOciMonitoringAlarm() {
      // When
      String alarm =
          AlertTriggerHelper.createOciMonitoringAlarm(
              "TestOciAlarm", "ocid1.instance.oc1.iad.test12345");

      // Then - should be valid JSON
      JsonObject json = parseJson(alarm);
      assertThat(json).isNotNull();

      // Then - verify OCI event structure
      assertThat(json.getString("type")).isEqualTo("com.oraclecloud.monitoring.alarmstatechange");
      assertThat(json.getString("source")).isEqualTo("MonitoringService");
      assertThat(json.containsKey("data")).isTrue();

      // Then - verify nested data
      JsonObject data = json.getJsonObject("data");
      assertThat(data.getString("alarmName")).isEqualTo("TestOciAlarm");
      assertThat(data.getString("currentState")).isEqualTo("FIRING");

      // Then - verify dimensions contain resource OCID
      JsonObject dimensions = data.getJsonObject("dimensions");
      assertThat(dimensions.getString("resourceId")).isEqualTo("ocid1.instance.oc1.iad.test12345");
    }

    @Test
    @DisplayName("Should throw for null alarm name")
    void shouldThrowForNullAlarmName() {
      assertThatThrownBy(() -> AlertTriggerHelper.createOciMonitoringAlarm(null, "ocid1.test"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("alarmName");
    }

    @Test
    @DisplayName("Should throw for null resource OCID")
    void shouldThrowForNullResourceOcid() {
      assertThatThrownBy(() -> AlertTriggerHelper.createOciMonitoringAlarm("TestAlarm", null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("resourceOcid");
    }
  }

  @Nested
  @DisplayName("wrapInSnsEnvelope")
  class WrapInSnsEnvelopeTests {

    @Test
    @DisplayName("Should wrap alert JSON in SNS notification envelope")
    void shouldWrapAlertInSnsEnvelope() {
      // Given - a simple CloudWatch alarm message
      String alarmMessage =
          """
          {
            "AlarmName": "TestAlarm",
            "NewStateValue": "ALARM",
            "NewStateReason": "Test reason"
          }
          """;

      // When
      String wrapped = AlertTriggerHelper.wrapInSnsEnvelope(alarmMessage);

      // Then - should be valid SNS envelope
      JsonObject json = parseJson(wrapped);
      assertThat(json.getString("Type")).isEqualTo("Notification");
      assertThat(json.containsKey("MessageId")).isTrue();
      assertThat(json.containsKey("TopicArn")).isTrue();
      assertThat(json.containsKey("Timestamp")).isTrue();

      // Then - Message should contain the original alarm (as escaped JSON)
      String message = json.getString("Message");
      JsonObject innerAlarm = parseJson(message);
      assertThat(innerAlarm.getString("AlarmName")).isEqualTo("TestAlarm");
    }

    @Test
    @DisplayName("Should throw for null message")
    void shouldThrowForNullMessage() {
      assertThatThrownBy(() -> AlertTriggerHelper.wrapInSnsEnvelope(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("message");
    }

    @Test
    @DisplayName("Should properly escape special characters in message")
    void shouldEscapeSpecialCharactersInMessage() {
      // Given - message with quotes and special chars
      String alarmMessage =
          """
          {
            "AlarmName": "Test\\"Alarm",
            "NewStateValue": "ALARM"
          }
          """;

      // When
      String wrapped = AlertTriggerHelper.wrapInSnsEnvelope(alarmMessage);

      // Then - should be parseable
      JsonObject json = parseJson(wrapped);
      assertThat(json.containsKey("Message")).isTrue();
    }
  }

  @Nested
  @DisplayName("parseFromSnsMessage")
  class ParseFromSnsMessageTests {

    @Test
    @DisplayName("Should extract CloudWatch alarm from SNS message")
    void shouldExtractCloudWatchAlarmFromSns() {
      // Given - SNS-wrapped alarm
      String snsMessage =
          """
          {
            "Type": "Notification",
            "MessageId": "test-123",
            "Message": "{\\"AlarmName\\":\\"TestAlarm\\",\\"NewStateValue\\":\\"ALARM\\",\\"NewStateReason\\":\\"Test\\"}"
          }
          """;

      // When
      JsonObject alarm = AlertTriggerHelper.parseFromSnsMessage(snsMessage);

      // Then
      assertThat(alarm.getString("AlarmName")).isEqualTo("TestAlarm");
      assertThat(alarm.getString("NewStateValue")).isEqualTo("ALARM");
    }

    @Test
    @DisplayName("Should throw for non-SNS message")
    void shouldThrowForNonSnsMessage() {
      String notSns =
          """
          {
            "AlarmName": "DirectAlarm",
            "NewStateValue": "ALARM"
          }
          """;

      assertThatThrownBy(() -> AlertTriggerHelper.parseFromSnsMessage(notSns))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("SNS");
    }

    @Test
    @DisplayName("Should throw for null message")
    void shouldThrowForNullMessage() {
      assertThatThrownBy(() -> AlertTriggerHelper.parseFromSnsMessage(null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw for missing Message field")
    void shouldThrowForMissingMessageField() {
      String snsWithoutMessage =
          """
          {
            "Type": "Notification",
            "MessageId": "test-123"
          }
          """;

      assertThatThrownBy(() -> AlertTriggerHelper.parseFromSnsMessage(snsWithoutMessage))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Message");
    }
  }

  // Helper method
  private JsonObject parseJson(String json) {
    try (JsonReader reader = Json.createReader(new StringReader(json))) {
      return reader.readObject();
    }
  }
}
