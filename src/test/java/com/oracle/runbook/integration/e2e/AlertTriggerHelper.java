package com.oracle.runbook.integration.e2e;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.util.UUID;

/**
 * Helper utility for creating alarm payloads in various formats for E2E testing.
 *
 * <p>Provides factory methods for:
 *
 * <ul>
 *   <li>CloudWatch Alarm payloads (wrapped in SNS envelope)
 *   <li>OCI Monitoring Alarm payloads
 *   <li>SNS envelope wrapping/unwrapping
 * </ul>
 */
public final class AlertTriggerHelper {

  private static final String DEFAULT_REGION = "us-west-2";
  private static final String DEFAULT_ACCOUNT_ID = "123456789012";
  private static final String DEFAULT_INSTANCE_ID = "i-test12345678";

  private AlertTriggerHelper() {
    // Utility class
  }

  /**
   * Creates a CloudWatch Alarm payload wrapped in SNS envelope.
   *
   * @param alarmName name of the alarm
   * @param metric metric name (e.g., "CPUUtilization", "MemoryUtilization")
   * @param threshold threshold value that was crossed
   * @return JSON string representing SNS-wrapped CloudWatch alarm
   * @throws IllegalArgumentException if alarmName is null or empty
   */
  public static String createCloudWatchAlarm(String alarmName, String metric, double threshold) {
    if (alarmName == null || alarmName.isBlank()) {
      throw new IllegalArgumentException("alarmName cannot be null or empty");
    }
    if (metric == null || metric.isBlank()) {
      throw new IllegalArgumentException("metric cannot be null or empty");
    }

    String alarmArn =
        String.format(
            "arn:aws:cloudwatch:%s:%s:alarm:%s", DEFAULT_REGION, DEFAULT_ACCOUNT_ID, alarmName);
    String topicArn =
        String.format("arn:aws:sns:%s:%s:cloudwatch-alarms", DEFAULT_REGION, DEFAULT_ACCOUNT_ID);
    Instant now = Instant.now();
    String timestamp = now.toString();

    // Build the inner CloudWatch alarm JSON
    JsonObject trigger =
        Json.createObjectBuilder()
            .add("MetricName", metric)
            .add("Namespace", "AWS/EC2")
            .add("StatisticType", "Statistic")
            .add("Statistic", "AVERAGE")
            .add("Period", 300)
            .add("EvaluationPeriods", 1)
            .add("DatapointsToAlarm", 1)
            .add("ComparisonOperator", "GreaterThanThreshold")
            .add("Threshold", threshold)
            .add("TreatMissingData", "missing")
            .add(
                "Dimensions",
                Json.createArrayBuilder()
                    .add(
                        Json.createObjectBuilder()
                            .add("name", "InstanceId")
                            .add("value", DEFAULT_INSTANCE_ID)))
            .build();

    JsonObject alarmJson =
        Json.createObjectBuilder()
            .add("AlarmName", alarmName)
            .add("AlarmDescription", String.format("%s exceeded %.1f threshold", metric, threshold))
            .add("AWSAccountId", DEFAULT_ACCOUNT_ID)
            .add("AlarmConfigurationUpdatedTimestamp", timestamp)
            .add("NewStateValue", "ALARM")
            .add(
                "NewStateReason",
                String.format(
                    "Threshold Crossed: 1 out of 1 datapoints was greater than the threshold (%.1f).",
                    threshold))
            .add("StateChangeTime", timestamp)
            .add("Region", "US West (Oregon)")
            .add("AlarmArn", alarmArn)
            .add("OldStateValue", "OK")
            .add("OKActions", Json.createArrayBuilder())
            .add("AlarmActions", Json.createArrayBuilder().add(topicArn))
            .add("InsufficientDataActions", Json.createArrayBuilder())
            .add("Trigger", trigger)
            .build();

    // Convert alarm JSON to string for embedding in SNS message
    String alarmJsonString = toJsonString(alarmJson);

    // Wrap in SNS envelope
    return wrapInSnsEnvelope(alarmJsonString);
  }

  /**
   * Creates an OCI Monitoring Alarm payload.
   *
   * @param alarmName name of the alarm
   * @param resourceOcid OCID of the resource triggering the alarm
   * @return JSON string representing OCI Monitoring alarm event
   * @throws IllegalArgumentException if alarmName or resourceOcid is null or empty
   */
  public static String createOciMonitoringAlarm(String alarmName, String resourceOcid) {
    if (alarmName == null || alarmName.isBlank()) {
      throw new IllegalArgumentException("alarmName cannot be null or empty");
    }
    if (resourceOcid == null || resourceOcid.isBlank()) {
      throw new IllegalArgumentException("resourceOcid cannot be null or empty");
    }

    Instant now = Instant.now();
    String alarmOcid = "ocid1.alarm.oc1.iad." + UUID.randomUUID().toString().substring(0, 8);

    JsonObject dimensions =
        Json.createObjectBuilder()
            .add("resourceId", resourceOcid)
            .add("availabilityDomain", "AD-1")
            .add("faultDomain", "FAULT-DOMAIN-1")
            .build();

    JsonObject messageObj =
        Json.createObjectBuilder()
            .add("title", alarmName)
            .add("body", "Alarm triggered. Immediate investigation required.")
            .build();

    JsonObject data =
        Json.createObjectBuilder()
            .add("alarmName", alarmName)
            .add("alarmId", alarmOcid)
            .add("compartmentId", "ocid1.compartment.oc1..example")
            .add("severity", "CRITICAL")
            .add("previousState", "OK")
            .add("currentState", "FIRING")
            .add("triggerTime", now.toString())
            .add("body", "Alarm condition detected on resource.")
            .add("dimensions", dimensions)
            .add("message", messageObj)
            .build();

    JsonObject event =
        Json.createObjectBuilder()
            .add("type", "com.oraclecloud.monitoring.alarmstatechange")
            .add("source", "MonitoringService")
            .add("specversion", "1.0")
            .add("id", UUID.randomUUID().toString())
            .add("time", now.toString())
            .add("subject", alarmOcid)
            .add("data", data)
            .build();

    return toJsonString(event);
  }

  /**
   * Wraps an alert message JSON in an SNS notification envelope.
   *
   * @param message the alert message JSON to wrap
   * @return JSON string representing SNS notification with message embedded
   * @throws IllegalArgumentException if message is null or empty
   */
  public static String wrapInSnsEnvelope(String message) {
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("message cannot be null or empty");
    }

    String messageId = "msg-" + UUID.randomUUID().toString().substring(0, 8);
    String topicArn =
        String.format("arn:aws:sns:%s:%s:cloudwatch-alarms", DEFAULT_REGION, DEFAULT_ACCOUNT_ID);
    Instant now = Instant.now();

    // The message needs to be a compact JSON string (no pretty printing)
    // and properly escaped when embedded in the outer JSON
    String compactMessage = compactJson(message);

    JsonObject envelope =
        Json.createObjectBuilder()
            .add("Type", "Notification")
            .add("MessageId", messageId)
            .add("TopicArn", topicArn)
            .add("Subject", "ALARM: CloudWatch Alarm triggered")
            .add("Message", compactMessage)
            .add("Timestamp", now.toString())
            .add("SignatureVersion", "1")
            .add("Signature", "test-signature==")
            .add("SigningCertURL", "https://sns." + DEFAULT_REGION + ".amazonaws.com/cert.pem")
            .add(
                "UnsubscribeURL",
                "https://sns." + DEFAULT_REGION + ".amazonaws.com/?Action=Unsubscribe")
            .build();

    return toJsonString(envelope);
  }

  /**
   * Extracts the CloudWatch alarm JSON from an SNS notification envelope.
   *
   * @param snsMessage the SNS notification JSON
   * @return JsonObject containing the extracted alarm payload
   * @throws IllegalArgumentException if not a valid SNS message or missing Message field
   */
  public static JsonObject parseFromSnsMessage(String snsMessage) {
    if (snsMessage == null || snsMessage.isBlank()) {
      throw new IllegalArgumentException("snsMessage cannot be null or empty");
    }

    JsonObject envelope;
    try {
      envelope = parseJson(snsMessage);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse SNS message JSON", e);
    }

    // Validate it's an SNS notification
    if (!"Notification".equals(envelope.getString("Type", null))) {
      throw new IllegalArgumentException("Not a valid SNS Notification - missing or invalid Type");
    }

    // Extract Message field
    if (!envelope.containsKey("Message")) {
      throw new IllegalArgumentException("SNS message is missing the Message field");
    }

    String messageContent = envelope.getString("Message");
    if (messageContent == null || messageContent.isBlank()) {
      throw new IllegalArgumentException("SNS Message field is empty");
    }

    try {
      return parseJson(messageContent);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse inner Message JSON", e);
    }
  }

  // ========== Private Helpers ==========

  private static JsonObject parseJson(String json) {
    try (JsonReader reader = Json.createReader(new StringReader(json))) {
      return reader.readObject();
    }
  }

  private static String toJsonString(JsonObject json) {
    StringWriter writer = new StringWriter();
    try (var jsonWriter = Json.createWriter(writer)) {
      jsonWriter.writeObject(json);
    }
    return writer.toString();
  }

  /** Compacts JSON by parsing and re-serializing without pretty printing. */
  private static String compactJson(String json) {
    try {
      JsonObject parsed = parseJson(json);
      return toJsonString(parsed);
    } catch (Exception e) {
      // If it's not valid JSON, just return as-is
      return json;
    }
  }
}
