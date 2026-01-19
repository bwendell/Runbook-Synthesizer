package com.oracle.runbook.infrastructure.cloud.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.domain.Alert;
import com.oracle.runbook.domain.AlertSeverity;
import com.oracle.runbook.integration.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AwsSnsAlertSourceAdapter.
 *
 * <p>Tests CloudWatch Alarm parsing from SNS webhook payloads following TDD approach.
 */
class AwsSnsAlertSourceAdapterTest {

  private AwsSnsAlertSourceAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new AwsSnsAlertSourceAdapter();
  }

  @Nested
  @DisplayName("sourceType()")
  class SourceTypeTests {

    @Test
    @DisplayName("returns 'aws-cloudwatch-sns'")
    void sourceType_returnsExpectedIdentifier() {
      assertThat(adapter.sourceType()).isEqualTo("aws-cloudwatch-sns");
    }
  }

  @Nested
  @DisplayName("canHandle()")
  class CanHandleTests {

    @Test
    @DisplayName("returns true for SNS CloudWatch notification")
    void canHandle_returnsTrueForSnsCloudWatchPayload() {
      String payload = TestFixtures.loadString("alerts/cloudwatch-alarm-sns.json");

      assertThat(adapter.canHandle(payload)).isTrue();
    }

    @Test
    @DisplayName("returns false for non-SNS payload")
    void canHandle_returnsFalseForNonSnsPayload() {
      String payload = "{\"alertname\":\"Test\",\"type\":\"prometheus\"}";

      assertThat(adapter.canHandle(payload)).isFalse();
    }

    @Test
    @DisplayName("returns false for SNS without CloudWatch alarm")
    void canHandle_returnsFalseForSnsWithoutCloudWatchAlarm() {
      String payload =
          """
          {
            "Type": "Notification",
            "TopicArn": "arn:aws:sns:us-west-2:123456789012:other-topic",
            "Message": "Plain text message, not a CloudWatch alarm"
          }
          """;

      assertThat(adapter.canHandle(payload)).isFalse();
    }

    @Test
    @DisplayName("returns false for null payload")
    void canHandle_returnsFalseForNull() {
      assertThat(adapter.canHandle(null)).isFalse();
    }

    @Test
    @DisplayName("returns false for empty payload")
    void canHandle_returnsFalseForEmpty() {
      assertThat(adapter.canHandle("")).isFalse();
    }

    @Test
    @DisplayName("returns false for invalid JSON")
    void canHandle_returnsFalseForInvalidJson() {
      assertThat(adapter.canHandle("not valid json")).isFalse();
    }
  }

  @Nested
  @DisplayName("parseAlert() - ALARM state")
  class ParseAlertAlarmStateTests {

    @Test
    @DisplayName("parses CloudWatch ALARM state to CRITICAL severity")
    void parseAlert_mapsAlarmToCriticalSeverity() {
      String payload = TestFixtures.loadString("alerts/cloudwatch-alarm-sns.json");

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert).isNotNull();
      assertThat(alert.severity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    @DisplayName("maps AlarmName to Alert.title")
    void parseAlert_mapsAlarmNameToTitle() {
      String payload = TestFixtures.loadString("alerts/cloudwatch-alarm-sns.json");

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert.title()).isEqualTo("HighCPUUtilization");
    }

    @Test
    @DisplayName("maps NewStateReason to Alert.message")
    void parseAlert_mapsNewStateReasonToMessage() {
      String payload = TestFixtures.loadString("alerts/cloudwatch-alarm-sns.json");

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert.message()).contains("Threshold Crossed");
    }

    @Test
    @DisplayName("sets sourceService to 'aws-cloudwatch-sns'")
    void parseAlert_setsSourceService() {
      String payload = TestFixtures.loadString("alerts/cloudwatch-alarm-sns.json");

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert.sourceService()).isEqualTo("aws-cloudwatch-sns");
    }

    @Test
    @DisplayName("extracts dimensions from Trigger")
    void parseAlert_extractsDimensionsFromTrigger() {
      String payload = TestFixtures.loadString("alerts/cloudwatch-alarm-sns.json");

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert.dimensions())
          .containsEntry("InstanceId", "i-1234567890abcdef0")
          .containsEntry("MetricName", "CPUUtilization")
          .containsEntry("Namespace", "AWS/EC2");
    }

    @Test
    @DisplayName("preserves raw payload")
    void parseAlert_preservesRawPayload() {
      String payload = TestFixtures.loadString("alerts/cloudwatch-alarm-sns.json");

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert.rawPayload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("generates unique alert ID from MessageId and AlarmArn")
    void parseAlert_generatesUniqueId() {
      String payload = TestFixtures.loadString("alerts/cloudwatch-alarm-sns.json");

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert.id()).isNotBlank();
      // ID should be deterministic for same message
      Alert secondAlert = adapter.parseAlert(payload);
      assertThat(alert.id()).isEqualTo(secondAlert.id());
    }

    @Test
    @DisplayName("parses timestamp from StateChangeTime")
    void parseAlert_parsesTimestamp() {
      String payload = TestFixtures.loadString("alerts/cloudwatch-alarm-sns.json");

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert.timestamp()).isNotNull();
    }
  }

  @Nested
  @DisplayName("parseAlert() - INSUFFICIENT_DATA state")
  class ParseAlertInsufficientDataTests {

    @Test
    @DisplayName("parses INSUFFICIENT_DATA state to WARNING severity")
    void parseAlert_mapsInsufficientDataToWarningSeverity() {
      String payload =
          TestFixtures.loadString("alerts/cloudwatch-alarm-sns-insufficient-data.json");

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert).isNotNull();
      assertThat(alert.severity()).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    @DisplayName("parses INSUFFICIENT_DATA alarm correctly")
    void parseAlert_parsesInsufficientDataAlarm() {
      String payload =
          TestFixtures.loadString("alerts/cloudwatch-alarm-sns-insufficient-data.json");

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert.title()).isEqualTo("MemoryUtilization");
      assertThat(alert.message()).contains("Insufficient Data");
    }
  }

  @Nested
  @DisplayName("parseAlert() - OK state")
  class ParseAlertOkStateTests {

    @Test
    @DisplayName("returns null for OK state (skip recovery alerts)")
    void parseAlert_returnsNullForOkState() {
      String payload = TestFixtures.loadString("alerts/cloudwatch-alarm-sns-ok.json");

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert).isNull();
    }
  }

  @Nested
  @DisplayName("parseAlert() - Error handling")
  class ParseAlertErrorHandlingTests {

    @Test
    @DisplayName("throws IllegalArgumentException for null payload")
    void parseAlert_throwsForNullPayload() {
      assertThatThrownBy(() -> adapter.parseAlert(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("payload");
    }

    @Test
    @DisplayName("throws IllegalArgumentException for empty payload")
    void parseAlert_throwsForEmptyPayload() {
      assertThatThrownBy(() -> adapter.parseAlert(""))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("payload");
    }

    @Test
    @DisplayName("throws IllegalArgumentException for invalid JSON")
    void parseAlert_throwsForInvalidJson() {
      assertThatThrownBy(() -> adapter.parseAlert("not valid json"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("parse");
    }

    @Test
    @DisplayName("throws IllegalArgumentException for non-CloudWatch SNS message")
    void parseAlert_throwsForNonCloudWatchMessage() {
      String payload =
          """
          {
            "Type": "Notification",
            "Message": "Plain text, not JSON"
          }
          """;

      assertThatThrownBy(() -> adapter.parseAlert(payload))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("throws IllegalArgumentException for missing SNS Message field")
    void parseAlert_throwsForMissingMessageField() {
      String payload =
          """
          {
            "Type": "Notification",
            "TopicArn": "arn:aws:sns:us-west-2:123456789012:topic"
          }
          """;

      assertThatThrownBy(() -> adapter.parseAlert(payload))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Message");
    }

    @Test
    @DisplayName("throws IllegalArgumentException for missing AlarmName in CloudWatch message")
    void parseAlert_throwsForMissingAlarmName() {
      String payload =
          """
          {
            "Type": "Notification",
            "MessageId": "test-123",
            "Message": "{\\"NewStateValue\\":\\"ALARM\\",\\"NewStateReason\\":\\"Test\\"}"
          }
          """;

      assertThatThrownBy(() -> adapter.parseAlert(payload))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("required");
    }

    @Test
    @DisplayName("throws IllegalArgumentException for missing NewStateValue in CloudWatch message")
    void parseAlert_throwsForMissingNewStateValue() {
      String payload =
          """
          {
            "Type": "Notification",
            "MessageId": "test-123",
            "Message": "{\\"AlarmName\\":\\"TestAlarm\\",\\"NewStateReason\\":\\"Test\\"}"
          }
          """;

      assertThatThrownBy(() -> adapter.parseAlert(payload))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("required");
    }
  }

  @Nested
  @DisplayName("parseAlert() - Edge cases with defaults")
  class ParseAlertEdgeCasesTests {

    @Test
    @DisplayName("uses INFO severity for unknown state values")
    void parseAlert_usesInfoSeverityForUnknownState() {
      String payload =
          """
          {
            "Type": "Notification",
            "MessageId": "test-123",
            "Message": "{\\"AlarmName\\":\\"TestAlarm\\",\\"NewStateValue\\":\\"UNKNOWN_STATE\\",\\"NewStateReason\\":\\"Unknown\\",\\"StateChangeTime\\":\\"2024-01-15T10:30:00.000+0000\\"}"
          }
          """;

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert).isNotNull();
      assertThat(alert.severity()).isEqualTo(AlertSeverity.INFO);
    }

    @Test
    @DisplayName("returns empty dimensions when Trigger is missing")
    void parseAlert_returnsEmptyDimensionsWhenNoTrigger() {
      String payload =
          """
          {
            "Type": "Notification",
            "MessageId": "test-123",
            "Message": "{\\"AlarmName\\":\\"TestAlarm\\",\\"NewStateValue\\":\\"ALARM\\",\\"NewStateReason\\":\\"Test\\",\\"StateChangeTime\\":\\"2024-01-15T10:30:00.000+0000\\"}"
          }
          """;

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert).isNotNull();
      assertThat(alert.dimensions()).isEmpty();
    }

    @Test
    @DisplayName("uses default message when NewStateReason is missing")
    void parseAlert_usesDefaultMessageWhenReasonMissing() {
      String payload =
          """
          {
            "Type": "Notification",
            "MessageId": "test-123",
            "Message": "{\\"AlarmName\\":\\"TestAlarm\\",\\"NewStateValue\\":\\"ALARM\\",\\"StateChangeTime\\":\\"2024-01-15T10:30:00.000+0000\\"}"
          }
          """;

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert).isNotNull();
      assertThat(alert.message()).isEqualTo("No reason provided");
    }

    @Test
    @DisplayName("generates valid ID when MessageId and AlarmArn are missing")
    void parseAlert_generatesIdWhenMessageIdAndAlarmArnMissing() {
      String payload =
          """
          {
            "Type": "Notification",
            "Message": "{\\"AlarmName\\":\\"TestAlarm\\",\\"NewStateValue\\":\\"ALARM\\",\\"NewStateReason\\":\\"Test\\",\\"StateChangeTime\\":\\"2024-01-15T10:30:00.000+0000\\"}"
          }
          """;

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert).isNotNull();
      assertThat(alert.id()).isNotBlank();
      assertThat(alert.id()).startsWith("cw-");
    }

    @Test
    @DisplayName("uses current time when StateChangeTime is missing")
    void parseAlert_usesCurrentTimeWhenStateChangeTimeMissing() {
      String payload =
          """
          {
            "Type": "Notification",
            "MessageId": "test-123",
            "Message": "{\\"AlarmName\\":\\"TestAlarm\\",\\"NewStateValue\\":\\"ALARM\\",\\"NewStateReason\\":\\"Test\\"}"
          }
          """;

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert).isNotNull();
      assertThat(alert.timestamp()).isNotNull();
      // Timestamp should be recent (within last few seconds)
      assertThat(alert.timestamp()).isAfter(java.time.Instant.now().minusSeconds(10));
    }

    @Test
    @DisplayName("handles ISO-8601 timestamp format fallback")
    void parseAlert_handlesIsoTimestampFormat() {
      String payload =
          """
          {
            "Type": "Notification",
            "MessageId": "test-123",
            "Message": "{\\"AlarmName\\":\\"TestAlarm\\",\\"NewStateValue\\":\\"ALARM\\",\\"NewStateReason\\":\\"Test\\",\\"StateChangeTime\\":\\"2024-01-15T10:30:00Z\\"}"
          }
          """;

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert).isNotNull();
      assertThat(alert.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("uses current time for unparseable timestamp")
    void parseAlert_usesCurrentTimeForUnparseableTimestamp() {
      String payload =
          """
          {
            "Type": "Notification",
            "MessageId": "test-123",
            "Message": "{\\"AlarmName\\":\\"TestAlarm\\",\\"NewStateValue\\":\\"ALARM\\",\\"NewStateReason\\":\\"Test\\",\\"StateChangeTime\\":\\"not-a-valid-timestamp\\"}"
          }
          """;

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert).isNotNull();
      assertThat(alert.timestamp()).isNotNull();
      assertThat(alert.timestamp()).isAfter(java.time.Instant.now().minusSeconds(10));
    }

    @Test
    @DisplayName("extracts dimensions even when some are null")
    void parseAlert_handlesPartialDimensions() {
      String payload =
          """
          {
            "Type": "Notification",
            "MessageId": "test-123",
            "Message": "{\\"AlarmName\\":\\"TestAlarm\\",\\"NewStateValue\\":\\"ALARM\\",\\"NewStateReason\\":\\"Test\\",\\"StateChangeTime\\":\\"2024-01-15T10:30:00.000+0000\\",\\"Trigger\\":{\\"MetricName\\":\\"CPUUtilization\\",\\"Dimensions\\":[{\\"name\\":\\"InstanceId\\",\\"value\\":\\"i-123\\"},{\\"name\\":null,\\"value\\":\\"ignored\\"},{\\"name\\":\\"valid\\",\\"value\\":null}]}}"
          }
          """;

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert).isNotNull();
      assertThat(alert.dimensions())
          .containsEntry("InstanceId", "i-123")
          .containsEntry("MetricName", "CPUUtilization")
          .doesNotContainKey("valid"); // null value should be skipped
    }

    @Test
    @DisplayName("handles Trigger without Dimensions array")
    void parseAlert_handlesTriggerWithoutDimensions() {
      String payload =
          """
          {
            "Type": "Notification",
            "MessageId": "test-123",
            "Message": "{\\"AlarmName\\":\\"TestAlarm\\",\\"NewStateValue\\":\\"ALARM\\",\\"NewStateReason\\":\\"Test\\",\\"StateChangeTime\\":\\"2024-01-15T10:30:00.000+0000\\",\\"Trigger\\":{\\"MetricName\\":\\"CPUUtilization\\",\\"Namespace\\":\\"AWS/EC2\\"}}"
          }
          """;

      Alert alert = adapter.parseAlert(payload);

      assertThat(alert).isNotNull();
      assertThat(alert.dimensions())
          .containsEntry("MetricName", "CPUUtilization")
          .containsEntry("Namespace", "AWS/EC2")
          .hasSize(2);
    }
  }
}
