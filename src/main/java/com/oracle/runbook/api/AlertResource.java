package com.oracle.runbook.api;

import com.oracle.runbook.api.dto.AlertRequest;
import com.oracle.runbook.api.dto.ChecklistResponse;
import com.oracle.runbook.api.dto.ChecklistResponse.ChecklistStepResponse;
import com.oracle.runbook.api.dto.ErrorResponse;
import com.oracle.runbook.domain.Alert;
import com.oracle.runbook.domain.AlertSeverity;
import com.oracle.runbook.domain.DynamicChecklist;
import com.oracle.runbook.output.WebhookDispatcher;
import com.oracle.runbook.rag.RagPipelineService;
import io.helidon.http.HeaderNames;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Alert ingestion resource providing POST /api/v1/alerts endpoint.
 *
 * <p>Receives alerts and returns generated troubleshooting checklists. Supports both stub mode (for
 * testing) and real mode (for production) via constructor configuration.
 */
public class AlertResource implements HttpService {

  private static final Logger LOGGER = Logger.getLogger(AlertResource.class.getName());
  private static final int DEFAULT_TOP_K = 5;

  private final RagPipelineService ragPipeline;
  private final WebhookDispatcher webhookDispatcher;
  private final boolean stubMode;

  /**
   * Creates an AlertResource with injected dependencies.
   *
   * @param ragPipeline the RAG pipeline service for generating checklists
   * @param webhookDispatcher the dispatcher for sending to webhooks
   * @param stubMode if true, uses stub data instead of real pipeline
   */
  public AlertResource(
      RagPipelineService ragPipeline, WebhookDispatcher webhookDispatcher, boolean stubMode) {
    this.ragPipeline = ragPipeline;
    this.webhookDispatcher = webhookDispatcher;
    this.stubMode = stubMode;
  }

  /**
   * Creates an AlertResource in stub mode for backward compatibility. This constructor is used by
   * existing tests and simple deployments.
   */
  public AlertResource() {
    this(null, null, true);
  }

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
      AlertSeverity severity;
      try {
        severity = AlertSeverity.fromString(alertRequest.severity());
      } catch (IllegalArgumentException e) {
        sendError(
            res,
            Status.BAD_REQUEST_400,
            "VALIDATION_ERROR",
            "Invalid severity: " + alertRequest.severity());
        return;
      }

      if (stubMode) {
        // Stub mode: use hardcoded checklist for testing
        var checklistResponse = generateStubChecklist(alertRequest);
        res.header(HeaderNames.CONTENT_TYPE, "application/json");
        res.send(toJson(checklistResponse));
      } else {
        // Real mode: process through RAG pipeline
        try {
          Alert alert = convertToAlert(alertRequest, severity);
          DynamicChecklist checklist = ragPipeline.processAlert(alert, DEFAULT_TOP_K).join();
          var checklistResponse = convertToResponse(checklist);

          // Send response to HTTP client first
          res.header(HeaderNames.CONTENT_TYPE, "application/json");
          res.send(toJson(checklistResponse));

          // Then dispatch to webhooks (fire-and-forget)
          if (webhookDispatcher != null) {
            webhookDispatcher.dispatch(checklist);
          }
        } catch (Exception e) {
          LOGGER.log(Level.SEVERE, "Error processing alert through RAG pipeline", e);
          sendError(
              res,
              Status.INTERNAL_SERVER_ERROR_500,
              "PIPELINE_ERROR",
              "Failed to process alert: " + e.getMessage());
          return;
        }
      }

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

  /**
   * Converts an AlertRequest DTO to the domain Alert model.
   *
   * @param request the incoming request DTO
   * @param severity the parsed severity enum
   * @return the domain Alert
   */
  private Alert convertToAlert(AlertRequest request, AlertSeverity severity) {
    return new Alert(
        UUID.randomUUID().toString(),
        request.title(),
        request.message(),
        severity,
        request.sourceService(),
        request.dimensions(),
        request.labels(),
        Instant.now(),
        request.rawPayload());
  }

  /**
   * Converts a DynamicChecklist domain model to the response DTO.
   *
   * @param checklist the domain checklist
   * @return the response DTO
   */
  private ChecklistResponse convertToResponse(DynamicChecklist checklist) {
    var steps =
        checklist.steps().stream()
            .map(
                step ->
                    new ChecklistStepResponse(
                        step.order(),
                        step.instruction(),
                        step.rationale(),
                        step.currentValue(),
                        step.expectedValue(),
                        step.priority() != null ? step.priority().name() : "MEDIUM",
                        step.commands()))
            .toList();

    return new ChecklistResponse(
        checklist.alertId(),
        checklist.summary(),
        steps,
        checklist.sourceRunbooks(),
        checklist.generatedAt(),
        checklist.llmProviderUsed());
  }

  private void sendError(ServerResponse res, Status status, String errorCode, String message) {
    var error =
        new ErrorResponse(
            UUID.randomUUID().toString(), errorCode, message, Instant.now(), Map.of());

    res.status(status);
    res.header(HeaderNames.CONTENT_TYPE, "application/json");
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
