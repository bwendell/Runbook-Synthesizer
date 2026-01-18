package com.oracle.runbook.integration.webhook;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.oracle.runbook.domain.*;
import com.oracle.runbook.integration.IntegrationTestBase;
import com.oracle.runbook.output.WebhookConfig;
import com.oracle.runbook.output.WebhookDestination;
import com.oracle.runbook.output.WebhookResult;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.testing.junit5.SetUpServer;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Slack Block Kit formatting output.
 *
 * <p>Tests verify that webhook payloads sent to Slack contain proper Block Kit structure with
 * sections, dividers, and formatted checklist items.
 */
class SlackFormattingIT extends IntegrationTestBase {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  SlackFormattingIT(WebServer server) {
    super(server);
  }

  @SetUpServer
  static void setup(WebServerConfig.Builder builder) {
    builder.routing(routing -> routing.get("/health", (req, res) -> res.send("OK")));
  }

  @BeforeEach
  void setUp() {
    resetWireMock();
  }

  @Test
  void sendToSlack_ProducesBlockKitStructure() throws Exception {
    // Given: Stub Slack webhook endpoint
    wireMockServer.stubFor(
        post(urlPathEqualTo("/services/slack-webhook"))
            .willReturn(aResponse().withStatus(200).withBody("ok")));

    // Given: A Slack-formatted webhook destination
    WebhookConfig slackConfig =
        WebhookConfig.builder()
            .name("slack-alerts")
            .type("slack")
            .url(wireMockBaseUrl() + "/services/slack-webhook")
            .build();

    BlockKitSlackDestination slackDestination = new BlockKitSlackDestination(slackConfig);

    // Given: A checklist to send
    DynamicChecklist checklist = createTestChecklist();

    // When: Send to Slack
    WebhookResult result = slackDestination.send(checklist).join();

    // Then: Request was successful
    assertThat(result.isSuccess()).isTrue();

    // Then: Capture and verify Block Kit structure
    List<LoggedRequest> requests =
        wireMockServer.findAll(postRequestedFor(urlPathEqualTo("/services/slack-webhook")));
    assertThat(requests).hasSize(1);

    String requestBody = requests.get(0).getBodyAsString();
    JsonNode payload = OBJECT_MAPPER.readTree(requestBody);

    // Then: Payload has blocks array
    assertThat(payload.has("blocks")).isTrue();
    JsonNode blocks = payload.get("blocks");
    assertThat(blocks.isArray()).isTrue();
    assertThat(blocks.size()).isGreaterThanOrEqualTo(2);

    // Then: First block is a header or section with title
    JsonNode firstBlock = blocks.get(0);
    assertThat(firstBlock.has("type")).isTrue();
    String firstBlockType = firstBlock.get("type").asText();
    assertThat(firstBlockType).isIn("header", "section");
  }

  @Test
  void sendToSlack_IncludesDividerBetweenSections() throws Exception {
    // Given: Stub Slack endpoint
    wireMockServer.stubFor(
        post(urlPathEqualTo("/services/slack-webhook")).willReturn(aResponse().withStatus(200)));

    WebhookConfig slackConfig =
        WebhookConfig.builder()
            .name("slack-with-dividers")
            .type("slack")
            .url(wireMockBaseUrl() + "/services/slack-webhook")
            .build();

    BlockKitSlackDestination slackDestination = new BlockKitSlackDestination(slackConfig);

    DynamicChecklist checklist = createTestChecklist();

    // When: Send to Slack
    slackDestination.send(checklist).join();

    // Then: Capture request
    List<LoggedRequest> requests =
        wireMockServer.findAll(postRequestedFor(urlPathEqualTo("/services/slack-webhook")));
    String requestBody = requests.get(0).getBodyAsString();
    JsonNode payload = OBJECT_MAPPER.readTree(requestBody);

    // Then: Payload contains at least one divider block
    JsonNode blocks = payload.get("blocks");
    boolean hasDivider = false;
    for (JsonNode block : blocks) {
      if ("divider".equals(block.get("type").asText())) {
        hasDivider = true;
        break;
      }
    }
    Objects.requireNonNull(
            assertThat(hasDivider).as("Payload should include at least one divider block"))
        .isTrue();
  }

  @Test
  void sendToSlack_FormatsChecklistStepsAsMarkdown() throws Exception {
    // Given: Stub Slack endpoint
    wireMockServer.stubFor(
        post(urlPathEqualTo("/services/slack-webhook")).willReturn(aResponse().withStatus(200)));

    WebhookConfig slackConfig =
        WebhookConfig.builder()
            .name("slack-markdown")
            .type("slack")
            .url(wireMockBaseUrl() + "/services/slack-webhook")
            .build();

    BlockKitSlackDestination slackDestination = new BlockKitSlackDestination(slackConfig);

    DynamicChecklist checklist = createTestChecklist();

    // When: Send to Slack
    slackDestination.send(checklist).join();

    // Then: Capture request
    List<LoggedRequest> requests =
        wireMockServer.findAll(postRequestedFor(urlPathEqualTo("/services/slack-webhook")));
    String requestBody = requests.get(0).getBodyAsString();

    // Then: Request body contains markdown-formatted step indicators
    assertThat(requestBody).contains("mrkdwn");
    // Slack uses emoji checkboxes like :white_check_mark: or checkbox characters
    assertThat(requestBody).containsAnyOf("[ ]", "☐", ":white_large_square:", "Step");
  }

  // ========== Helper Methods ==========

  private DynamicChecklist createTestChecklist() {
    ChecklistStep step1 =
        new ChecklistStep(
            1,
            "Check memory usage with free -h",
            "Determine current memory state",
            "92%",
            "<80%",
            StepPriority.HIGH,
            List.of("free -h", "top -bn1 | head -20"));

    ChecklistStep step2 =
        new ChecklistStep(
            2,
            "Review application logs",
            "Look for memory-related errors",
            null,
            null,
            StepPriority.MEDIUM,
            List.of("journalctl -u myapp --since '1 hour ago'"));

    ChecklistStep step3 =
        new ChecklistStep(
            3,
            "Consider restarting if OOM detected",
            "Last resort if memory cannot be freed",
            null,
            null,
            StepPriority.LOW,
            List.of());

    return new DynamicChecklist(
        "alert-slack-001",
        "High Memory Alert: prod-app-server-01",
        List.of(step1, step2, step3),
        List.of("runbooks/memory-troubleshooting.md"),
        Instant.now(),
        "oci-genai");
  }

  /**
   * Test Slack destination that produces Block Kit formatted payloads. This simulates what the v1.1
   * SlackWebhookDestination will do.
   */
  private class BlockKitSlackDestination implements WebhookDestination {
    private final WebhookConfig config;

    BlockKitSlackDestination(WebhookConfig config) {
      this.config = config;
    }

    @Override
    public String name() {
      return config.name();
    }

    @Override
    public String type() {
      return "slack";
    }

    @Override
    public WebhookConfig config() {
      return config;
    }

    @Override
    public CompletableFuture<WebhookResult> send(DynamicChecklist checklist) {
      return CompletableFuture.supplyAsync(
          () -> {
            try {
              String blockKitPayload = formatAsBlockKit(checklist);

              io.helidon.webclient.api.WebClient client =
                  io.helidon.webclient.api.WebClient.builder().baseUri(config.url()).build();

              try (var response =
                  client
                      .post()
                      .header(io.helidon.http.HeaderNames.CONTENT_TYPE, "application/json")
                      .submit(blockKitPayload)) {
                int status = response.status().code();
                if (status >= 200 && status < 300) {
                  return WebhookResult.success(config.name(), status);
                }
                return WebhookResult.failure(config.name(), status, "HTTP " + status);
              }
            } catch (Exception e) {
              return WebhookResult.failure(config.name(), e.getMessage());
            }
          });
    }

    @Override
    public boolean shouldSend(DynamicChecklist checklist) {
      return true;
    }

    private String formatAsBlockKit(DynamicChecklist checklist) {
      StringBuilder blocks = new StringBuilder();
      blocks.append("{\"blocks\": [");

      // Header block
      blocks.append("{\"type\": \"header\", \"text\": {\"type\": \"plain_text\", \"text\": \"");
      blocks.append(escapeJson(checklist.summary()));
      blocks.append("\"}},");

      // Divider
      blocks.append("{\"type\": \"divider\"},");

      // Steps as section blocks
      for (ChecklistStep step : checklist.steps()) {
        blocks.append("{\"type\": \"section\", \"text\": {\"type\": \"mrkdwn\", \"text\": \"");
        blocks.append("☐ *Step ").append(step.order()).append(":* ");
        blocks.append(escapeJson(step.instruction()));
        if (step.currentValue() != null) {
          blocks.append(" _(Current: ").append(escapeJson(step.currentValue())).append(")_");
        }
        blocks.append("\"}},");
      }

      // Remove trailing comma and close array
      if (blocks.charAt(blocks.length() - 1) == ',') {
        blocks.setLength(blocks.length() - 1);
      }
      blocks.append("]}");

      return blocks.toString();
    }

    private String escapeJson(String text) {
      if (text == null) return "";
      return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
  }
}
