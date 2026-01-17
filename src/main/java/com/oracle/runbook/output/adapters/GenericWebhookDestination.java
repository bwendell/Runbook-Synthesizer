package com.oracle.runbook.output.adapters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oracle.runbook.domain.DynamicChecklist;
import com.oracle.runbook.output.WebhookConfig;
import com.oracle.runbook.output.WebhookDestination;
import com.oracle.runbook.output.WebhookResult;
import io.helidon.http.HeaderNames;
import io.helidon.webclient.api.HttpClientRequest;
import io.helidon.webclient.api.HttpClientResponse;
import io.helidon.webclient.api.WebClient;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Generic webhook destination that sends JSON-serialized checklists via HTTP POST.
 *
 * <p>This adapter works with any HTTP endpoint that accepts JSON payloads. Headers from the
 * configuration are included in all requests.
 */
public class GenericWebhookDestination implements WebhookDestination {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  private final WebhookConfig config;
  private final WebClient webClient;

  /**
   * Creates a new GenericWebhookDestination with the given configuration.
   *
   * @param config the webhook configuration
   */
  public GenericWebhookDestination(WebhookConfig config) {
    this.config = config;
    this.webClient =
        WebClient.builder()
            .connectTimeout(CONNECT_TIMEOUT)
            .readTimeout(READ_TIMEOUT)
            .baseUri(config.url())
            .build();
  }

  @Override
  public String name() {
    return config.name();
  }

  @Override
  public String type() {
    return config.type();
  }

  @Override
  public WebhookConfig config() {
    return config;
  }

  @Override
  public CompletableFuture<WebhookResult> send(DynamicChecklist checklist) {
    return CompletableFuture.supplyAsync(() -> sendSync(checklist));
  }

  private WebhookResult sendSync(DynamicChecklist checklist) {
    try {
      String jsonBody = OBJECT_MAPPER.writeValueAsString(checklist);

      HttpClientRequest request =
          webClient.post().header(HeaderNames.CONTENT_TYPE, "application/json");

      // Add configured headers
      config.headers().forEach((name, value) -> request.header(HeaderNames.create(name), value));

      try (HttpClientResponse response = request.submit(jsonBody)) {
        int statusCode = response.status().code();

        if (statusCode >= 200 && statusCode < 300) {
          return WebhookResult.success(config.name(), statusCode);
        } else {
          return WebhookResult.failure(
              config.name(), "HTTP " + statusCode + ": " + response.status().reasonPhrase());
        }
      }
    } catch (JsonProcessingException e) {
      return WebhookResult.failure(config.name(), "JSON serialization error: " + e.getMessage());
    } catch (Exception e) {
      return WebhookResult.failure(config.name(), "Connection error: " + e.getMessage());
    }
  }

  @Override
  public boolean shouldSend(DynamicChecklist checklist) {
    // For generic webhooks, we'd need alert info for filtering
    // Default to true (send all) since we don't have severity info on checklist
    // directly
    return true;
  }
}
