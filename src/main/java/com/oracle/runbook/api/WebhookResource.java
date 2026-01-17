package com.oracle.runbook.api;

import com.oracle.runbook.api.dto.ErrorResponse;
import com.oracle.runbook.api.dto.WebhookConfig;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Webhook configuration resource providing GET/POST /api/v1/webhooks endpoints.
 *
 * <p>Manages webhook destinations for checklist notifications.
 */
public class WebhookResource implements HttpService {

  private final List<WebhookConfig> webhooks = new ArrayList<>();

  @Override
  public void routing(HttpRules rules) {
    rules.get("/", this::handleGet);
    rules.post("/", this::handlePost);
  }

  private void handleGet(ServerRequest req, ServerResponse res) {
    var arrayBuilder = Json.createArrayBuilder();
    for (var webhook : webhooks) {
      arrayBuilder.add(toJsonObject(webhook));
    }
    res.send(arrayBuilder.build().toString());
  }

  private void handlePost(ServerRequest req, ServerResponse res) {
    try {
      String body = req.content().as(String.class);
      WebhookConfig config = parseWebhookConfig(body);
      webhooks.add(config);

      res.status(Status.CREATED_201);
      res.send(toJson(config));

    } catch (NullPointerException e) {
      sendError(res, Status.BAD_REQUEST_400, "VALIDATION_ERROR", e.getMessage());
    } catch (Exception e) {
      sendError(
          res, Status.BAD_REQUEST_400, "VALIDATION_ERROR", "Invalid request: " + e.getMessage());
    }
  }

  private WebhookConfig parseWebhookConfig(String body) {
    try (JsonReader reader = Json.createReader(new StringReader(body))) {
      JsonObject json = reader.readObject();

      return new WebhookConfig(
          json.containsKey("name") ? json.getString("name") : null,
          json.containsKey("type") ? json.getString("type") : null,
          json.containsKey("url") ? json.getString("url") : null,
          json.containsKey("enabled") && json.getBoolean("enabled"),
          parseStringList(json, "filterSeverities"),
          parseStringMap(json, "headers"));
    }
  }

  private List<String> parseStringList(JsonObject json, String key) {
    if (!json.containsKey(key) || json.isNull(key)) {
      return List.of();
    }
    JsonArray arr = json.getJsonArray(key);
    return arr.stream().map(v -> v.toString().replace("\"", "")).toList();
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

  private JsonObject toJsonObject(WebhookConfig config) {
    var severitiesBuilder = Json.createArrayBuilder();
    config.filterSeverities().forEach(severitiesBuilder::add);

    var headersBuilder = Json.createObjectBuilder();
    config.headers().forEach(headersBuilder::add);

    return Json.createObjectBuilder()
        .add("name", config.name())
        .add("type", config.type() != null ? config.type() : "")
        .add("url", config.url())
        .add("enabled", config.enabled())
        .add("filterSeverities", severitiesBuilder)
        .add("headers", headersBuilder)
        .build();
  }

  private String toJson(WebhookConfig config) {
    return toJsonObject(config).toString();
  }

  private void sendError(ServerResponse res, Status status, String errorCode, String message) {
    var error =
        new ErrorResponse(
            UUID.randomUUID().toString(), errorCode, message, Instant.now(), Map.of());

    var detailsBuilder = Json.createObjectBuilder();
    error.details().forEach(detailsBuilder::add);

    String json =
        Json.createObjectBuilder()
            .add("correlationId", error.correlationId())
            .add("errorCode", error.errorCode())
            .add("message", error.message() != null ? error.message() : "")
            .add("timestamp", error.timestamp().toString())
            .add("details", detailsBuilder)
            .build()
            .toString();

    res.status(status);
    res.send(json);
  }
}
