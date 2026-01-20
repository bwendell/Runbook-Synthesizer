package com.oracle.runbook.output.adapters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oracle.runbook.domain.DynamicChecklist;
import com.oracle.runbook.output.WebhookConfig;
import com.oracle.runbook.output.WebhookDestination;
import com.oracle.runbook.output.WebhookResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * File-based output adapter that writes checklists as JSON files to a configurable directory.
 *
 * <p>This adapter implements the {@link WebhookDestination} interface for consistency with other
 * output adapters, but writes to the local filesystem instead of making HTTP calls. This is useful
 * for MVP validation and local testing.
 *
 * <p>Output files are named using the pattern: {@code checklist-{alertId}-{timestamp}.json}
 *
 * @see FileOutputConfig
 */
public class FileOutputAdapter implements WebhookDestination {

  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .enable(SerializationFeature.INDENT_OUTPUT)
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private final FileOutputConfig config;
  private final WebhookConfig webhookConfig;

  /**
   * Creates a new FileOutputAdapter with the given configuration.
   *
   * @param config the file output configuration
   */
  public FileOutputAdapter(FileOutputConfig config) {
    this.config = config;
    // Create a synthetic WebhookConfig to satisfy the interface
    // Using file:// URL which bypasses HTTP/HTTPS validation
    this.webhookConfig =
        new WebhookConfig(
            config.name(),
            "file",
            "file://" + config.outputDirectory(),
            true, // enabled
            null, // filter - will default to allowAll
            Map.of(),
            WebhookConfig.DEFAULT_RETRY_COUNT,
            WebhookConfig.DEFAULT_RETRY_DELAY_MS);
  }

  @Override
  public String name() {
    return config.name();
  }

  @Override
  public String type() {
    return "file";
  }

  @Override
  public WebhookConfig config() {
    return webhookConfig;
  }

  @Override
  public CompletableFuture<WebhookResult> send(DynamicChecklist checklist) {
    return CompletableFuture.supplyAsync(() -> writeSync(checklist));
  }

  private WebhookResult writeSync(DynamicChecklist checklist) {
    try {
      Path outputDir = Path.of(config.outputDirectory());

      // Create directory if it doesn't exist
      Files.createDirectories(outputDir);

      // Generate filename with alertId and timestamp
      String filename = generateFilename(checklist.alertId());
      Path targetFile = outputDir.resolve(filename);

      // Serialize and write
      String jsonContent = OBJECT_MAPPER.writeValueAsString(checklist);
      Files.writeString(targetFile, jsonContent);

      return WebhookResult.success(config.name(), 200);
    } catch (JsonProcessingException e) {
      return WebhookResult.failure(config.name(), "JSON serialization error: " + e.getMessage());
    } catch (IOException e) {
      return WebhookResult.failure(config.name(), "IO error: " + e.getMessage());
    }
  }

  private String generateFilename(String alertId) {
    String timestamp = TIMESTAMP_FORMATTER.format(Instant.now());
    // Sanitize alertId for use in filename (replace invalid chars)
    String safeAlertId = alertId.replaceAll("[^a-zA-Z0-9-_]", "_");
    return String.format("checklist-%s-%s.json", safeAlertId, timestamp);
  }

  @Override
  public boolean shouldSend(DynamicChecklist checklist) {
    // File output always accepts all checklists
    return true;
  }
}
