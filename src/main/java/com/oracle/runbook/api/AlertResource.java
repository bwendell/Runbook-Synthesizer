package com.oracle.runbook.api;

import com.oracle.runbook.api.dto.AlertRequest;
import com.oracle.runbook.api.dto.ChecklistResponse;
import com.oracle.runbook.api.dto.ChecklistResponse.ChecklistStepResponse;
import com.oracle.runbook.api.dto.ErrorResponse;
import com.oracle.runbook.domain.AlertSeverity;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Alert ingestion resource providing POST /api/v1/alerts endpoint.
 *
 * <p>Receives alerts and returns generated troubleshooting checklists.
 */
public class AlertResource implements HttpService {

  @Override
  public void routing(HttpRules rules) {
    rules.post("/", this::handlePost);
  }

  private void handlePost(ServerRequest req, ServerResponse res) {
    try {
      String body = req.content().as(String.class);

      // Parse JSON request
      AlertRequest alertRequest = parseAlertRequest(body);

      // Validate severity
      try {
        AlertSeverity.fromString(alertRequest.severity());
      } catch (IllegalArgumentException e) {
        sendError(
            res,
            Status.BAD_REQUEST_400,
            "VALIDATION_ERROR",
            "Invalid severity: " + alertRequest.severity());
        return;
      }

      // Generate stub checklist response
      var checklistResponse = generateStubChecklist(alertRequest);

      // Send response
      res.send(toJson(checklistResponse));

    } catch (NullPointerException e) {
      sendError(res, Status.BAD_REQUEST_400, "VALIDATION_ERROR", e.getMessage());
    } catch (Exception e) {
      sendError(
          res, Status.BAD_REQUEST_400, "VALIDATION_ERROR", "Invalid request: " + e.getMessage());
    }
  }

  private AlertRequest parseAlertRequest(String body) {
    try (JsonReader reader = Json.createReader(new StringReader(body))) {
      JsonObject json = reader.readObject();

      return new AlertRequest(
          json.containsKey("title") ? json.getString("title") : null,
          json.containsKey("message") ? json.getString("message") : null,
          json.containsKey("severity") ? json.getString("severity") : null,
          json.containsKey("sourceService") ? json.getString("sourceService") : null,
          parseStringMap(json, "dimensions"),
          parseStringMap(json, "labels"),
          body);
    }
  }

  private Map<String, String> parseStringMap(JsonObject json, String key) {
    if (!json.containsKey(key) || json.isNull(key)) {
      return Map.of();
    }
    JsonObject obj = json.getJsonObject(key);
    var builder = new java.util.HashMap<String, String>();
    obj.keySet().forEach(k -> builder.put(k, obj.getString(k)));
    return Map.copyOf(builder);
  }

  private ChecklistResponse generateStubChecklist(AlertRequest request) {
    String alertId = UUID.randomUUID().toString();
    var steps =
        List.of(
            new ChecklistStepResponse(
                1,
                "Check current status",
                "First step in troubleshooting",
                "Unknown",
                "Healthy",
                "HIGH",
                List.of()));

    return new ChecklistResponse(
        alertId,
        "Generated checklist for: " + request.title(),
        steps,
        List.of("runbook-stub.md"),
        Instant.now(),
        "stub");
  }

  private void sendError(ServerResponse res, Status status, String errorCode, String message) {
    var error =
        new ErrorResponse(
            UUID.randomUUID().toString(), errorCode, message, Instant.now(), Map.of());

    res.status(status);
    res.send(toJson(error));
  }

  private String toJson(ChecklistResponse response) {
    var stepsBuilder = Json.createArrayBuilder();
    for (var step : response.steps()) {
      var commandsBuilder = Json.createArrayBuilder();
      step.commands().forEach(commandsBuilder::add);

      stepsBuilder.add(
          Json.createObjectBuilder()
              .add("order", step.order())
              .add("instruction", step.instruction())
              .add("rationale", step.rationale() != null ? step.rationale() : "")
              .add("currentValue", step.currentValue() != null ? step.currentValue() : "")
              .add("expectedValue", step.expectedValue() != null ? step.expectedValue() : "")
              .add("priority", step.priority() != null ? step.priority() : "")
              .add("commands", commandsBuilder));
    }

    var runbooksBuilder = Json.createArrayBuilder();
    response.sourceRunbooks().forEach(runbooksBuilder::add);

    return Json.createObjectBuilder()
        .add("alertId", response.alertId())
        .add("summary", response.summary() != null ? response.summary() : "")
        .add("steps", stepsBuilder)
        .add("sourceRunbooks", runbooksBuilder)
        .add("generatedAt", response.generatedAt().toString())
        .add(
            "llmProviderUsed", response.llmProviderUsed() != null ? response.llmProviderUsed() : "")
        .build()
        .toString();
  }

  private String toJson(ErrorResponse error) {
    var detailsBuilder = Json.createObjectBuilder();
    error.details().forEach(detailsBuilder::add);

    return Json.createObjectBuilder()
        .add("correlationId", error.correlationId())
        .add("errorCode", error.errorCode())
        .add("message", error.message() != null ? error.message() : "")
        .add("timestamp", error.timestamp().toString())
        .add("details", detailsBuilder)
        .build()
        .toString();
  }
}
