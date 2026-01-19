package com.oracle.runbook.infrastructure.cloud.aws;

import com.oracle.runbook.domain.Alert;
import com.oracle.runbook.domain.AlertSeverity;
import com.oracle.runbook.ingestion.AlertSourceAdapter;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Alert source adapter for CloudWatch Alarms delivered via SNS webhook.
 *
 * <p>Parses SNS notification envelope containing CloudWatch Alarm JSON payload.
 *
 * <p>Severity mapping:
 *
 * <ul>
 *   <li>{@code ALARM} → {@code CRITICAL}
 *   <li>{@code INSUFFICIENT_DATA} → {@code WARNING}
 *   <li>{@code OK} → returns null (skip recovery alerts)
 * </ul>
 */
public class AwsSnsAlertSourceAdapter implements AlertSourceAdapter {

  private static final String SOURCE_TYPE = "aws-cloudwatch-sns";
  private static final DateTimeFormatter CLOUDWATCH_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  @Override
  public String sourceType() {
    return SOURCE_TYPE;
  }

  @Override
  public boolean canHandle(String rawPayload) {
    if (rawPayload == null || rawPayload.isBlank()) {
      return false;
    }
    try {
      JsonObject json = parseJson(rawPayload);
      // Must be an SNS notification
      if (!"Notification".equals(json.getString("Type", null))) {
        return false;
      }
      // Must contain Message field
      String message = json.getString("Message", null);
      if (message == null || message.isBlank()) {
        return false;
      }
      // Message must be a CloudWatch Alarm (contains AlarmName and NewStateValue)
      try {
        JsonObject alarmJson = parseJson(message);
        return alarmJson.containsKey("AlarmName") && alarmJson.containsKey("NewStateValue");
      } catch (Exception e) {
        return false;
      }
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public Alert parseAlert(String rawPayload) {
    if (rawPayload == null || rawPayload.isBlank()) {
      throw new IllegalArgumentException("Alert payload cannot be null or empty");
    }

    JsonObject snsEnvelope;
    try {
      snsEnvelope = parseJson(rawPayload);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse SNS envelope JSON", e);
    }

    String messageString = snsEnvelope.getString("Message", null);
    if (messageString == null || messageString.isBlank()) {
      throw new IllegalArgumentException("SNS Message field is missing or empty");
    }

    JsonObject alarmJson;
    try {
      alarmJson = parseJson(messageString);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Failed to parse CloudWatch Alarm JSON from Message field", e);
    }

    // Validate required CloudWatch Alarm fields
    if (!alarmJson.containsKey("AlarmName") || !alarmJson.containsKey("NewStateValue")) {
      throw new IllegalArgumentException("Missing required CloudWatch Alarm fields");
    }

    String newStateValue = alarmJson.getString("NewStateValue");

    // Skip OK state (recovery alerts)
    if ("OK".equals(newStateValue)) {
      return null;
    }

    // Map severity
    AlertSeverity severity = mapSeverity(newStateValue);

    // Extract alarm details
    String alarmName = alarmJson.getString("AlarmName");
    String newStateReason = alarmJson.getString("NewStateReason", "No reason provided");
    String alarmArn = alarmJson.getString("AlarmArn", "");
    String messageId = snsEnvelope.getString("MessageId", "");

    // Generate deterministic alert ID from MessageId and AlarmArn
    String alertId = generateAlertId(messageId, alarmArn);

    // Parse timestamp from StateChangeTime
    Instant timestamp = parseTimestamp(alarmJson.getString("StateChangeTime", null));

    // Extract dimensions from Trigger
    Map<String, String> dimensions = extractDimensions(alarmJson);

    return new Alert(
        alertId,
        alarmName,
        newStateReason,
        severity,
        SOURCE_TYPE,
        dimensions,
        Map.of(), // labels (could extract from alarm description or tags)
        timestamp,
        rawPayload);
  }

  private AlertSeverity mapSeverity(String newStateValue) {
    return switch (newStateValue) {
      case "ALARM" -> AlertSeverity.CRITICAL;
      case "INSUFFICIENT_DATA" -> AlertSeverity.WARNING;
      default -> AlertSeverity.INFO;
    };
  }

  private String generateAlertId(String messageId, String alarmArn) {
    // Use hash of concatenated IDs for deterministic, unique ID
    String combined = messageId + ":" + alarmArn;
    return "cw-" + Integer.toHexString(combined.hashCode());
  }

  private Instant parseTimestamp(String stateChangeTime) {
    if (stateChangeTime == null || stateChangeTime.isBlank()) {
      return Instant.now();
    }
    try {
      // CloudWatch uses format like "2024-01-15T10:30:00.000+0000"
      return CLOUDWATCH_DATE_FORMAT.parse(stateChangeTime, Instant::from);
    } catch (DateTimeParseException e) {
      // Try ISO instant format as fallback
      try {
        return Instant.parse(stateChangeTime);
      } catch (DateTimeParseException e2) {
        return Instant.now();
      }
    }
  }

  private Map<String, String> extractDimensions(JsonObject alarmJson) {
    Map<String, String> dimensions = new HashMap<>();

    if (!alarmJson.containsKey("Trigger")) {
      return dimensions;
    }

    JsonObject trigger = alarmJson.getJsonObject("Trigger");

    // Add metric metadata
    if (trigger.containsKey("MetricName")) {
      dimensions.put("MetricName", trigger.getString("MetricName"));
    }
    if (trigger.containsKey("Namespace")) {
      dimensions.put("Namespace", trigger.getString("Namespace"));
    }

    // Add dimensions from the trigger
    if (trigger.containsKey("Dimensions")) {
      JsonArray triggerDimensions = trigger.getJsonArray("Dimensions");
      for (int i = 0; i < triggerDimensions.size(); i++) {
        JsonObject dim = triggerDimensions.getJsonObject(i);
        String name = dim.getString("name", null);
        String value = dim.getString("value", null);
        if (name != null && value != null) {
          dimensions.put(name, value);
        }
      }
    }

    return dimensions;
  }

  private JsonObject parseJson(String json) {
    try (JsonReader reader = Json.createReader(new StringReader(json))) {
      return reader.readObject();
    }
  }
}
