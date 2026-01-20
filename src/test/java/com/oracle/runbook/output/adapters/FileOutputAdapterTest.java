package com.oracle.runbook.output.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oracle.runbook.domain.ChecklistStep;
import com.oracle.runbook.domain.DynamicChecklist;
import com.oracle.runbook.domain.StepPriority;
import com.oracle.runbook.output.WebhookResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link FileOutputAdapter}.
 *
 * <p>Tests follow TDD RED-GREEN-REFACTOR cycle.
 */
class FileOutputAdapterTest {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  @TempDir Path tempDir;

  private FileOutputConfig config;
  private FileOutputAdapter adapter;

  @BeforeEach
  void setUp() {
    config = new FileOutputConfig(tempDir.toString(), "file-output");
    adapter = new FileOutputAdapter(config);
  }

  @Nested
  @DisplayName("WebhookDestination interface implementation")
  class InterfaceImplementation {

    @Test
    @DisplayName("name() returns configured name")
    void name_returnsConfiguredName() {
      assertThat(adapter.name()).isEqualTo("file-output");
    }

    @Test
    @DisplayName("type() returns 'file'")
    void type_returnsFile() {
      assertThat(adapter.type()).isEqualTo("file");
    }

    @Test
    @DisplayName("config() returns non-null WebhookConfig")
    void config_returnsNonNullConfig() {
      assertThat(adapter.config()).isNotNull();
    }

    @Test
    @DisplayName("shouldSend() always returns true")
    void shouldSend_alwaysReturnsTrue() {
      DynamicChecklist checklist = createSampleChecklist("alert-123");
      assertThat(adapter.shouldSend(checklist)).isTrue();
    }
  }

  @Nested
  @DisplayName("send() - successful writes")
  class SuccessfulWrites {

    @Test
    @DisplayName("send() writes checklist as JSON to configured directory")
    void send_writesChecklistAsJson() throws Exception {
      DynamicChecklist checklist = createSampleChecklist("alert-001");

      CompletableFuture<WebhookResult> future = adapter.send(checklist);
      WebhookResult result = future.join();

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.destinationName()).isEqualTo("file-output");

      // Verify file was created
      Path[] files = Files.list(tempDir).toArray(Path[]::new);
      assertThat(files).hasSize(1);

      // Verify file content is valid JSON
      String content = Files.readString(files[0]);
      DynamicChecklist parsed = OBJECT_MAPPER.readValue(content, DynamicChecklist.class);
      assertThat(parsed.alertId()).isEqualTo("alert-001");
    }

    @Test
    @DisplayName("send() creates filename with alertId and timestamp")
    void send_createsFilenameWithAlertIdAndTimestamp() throws Exception {
      DynamicChecklist checklist = createSampleChecklist("my-alert-id");

      adapter.send(checklist).join();

      Path[] files = Files.list(tempDir).toArray(Path[]::new);
      assertThat(files).hasSize(1);

      String filename = files[0].getFileName().toString();
      assertThat(filename).startsWith("checklist-my-alert-id-");
      assertThat(filename).endsWith(".json");
    }

    @Test
    @DisplayName("send() returns status code 200 on success")
    void send_returnsStatusCode200OnSuccess() {
      DynamicChecklist checklist = createSampleChecklist("alert-200");

      WebhookResult result = adapter.send(checklist).join();

      assertThat(result.statusCode()).isEqualTo(200);
    }
  }

  @Nested
  @DisplayName("send() - directory creation")
  class DirectoryCreation {

    @Test
    @DisplayName("send() creates output directory if it doesn't exist")
    void send_createsDirectoryIfNotExists() throws Exception {
      Path nestedDir = tempDir.resolve("nested").resolve("output");
      FileOutputConfig nestedConfig = new FileOutputConfig(nestedDir.toString(), "nested-output");
      FileOutputAdapter nestedAdapter = new FileOutputAdapter(nestedConfig);

      DynamicChecklist checklist = createSampleChecklist("alert-nested");

      WebhookResult result = nestedAdapter.send(checklist).join();

      assertThat(result.isSuccess()).isTrue();
      assertThat(Files.exists(nestedDir)).isTrue();
      assertThat(Files.list(nestedDir).count()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("send() - error handling")
  class ErrorHandling {

    @Test
    @DisplayName("send() returns failure result on IO error")
    void send_returnsFailureOnIoError() throws Exception {
      // Create a file where the directory should be to cause an error
      Path blockingFile = tempDir.resolve("blocking-path");
      Files.createFile(blockingFile);

      FileOutputConfig badConfig = new FileOutputConfig(blockingFile.toString(), "blocked-output");
      FileOutputAdapter badAdapter = new FileOutputAdapter(badConfig);

      DynamicChecklist checklist = createSampleChecklist("alert-error");

      WebhookResult result = badAdapter.send(checklist).join();

      assertThat(result.isSuccess()).isFalse();
      assertThat(result.destinationName()).isEqualTo("blocked-output");
      assertThat(result.errorMessage()).isPresent();
    }
  }

  @Nested
  @DisplayName("FileOutputConfig validation")
  class ConfigValidation {

    @Test
    @DisplayName("FileOutputConfig throws for null outputDirectory")
    void config_throwsForNullOutputDirectory() {
      assertThatThrownBy(() -> new FileOutputConfig(null, "test"))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("FileOutputConfig throws for null name")
    void config_throwsForNullName() {
      assertThatThrownBy(() -> new FileOutputConfig("/path", null))
          .isInstanceOf(NullPointerException.class);
    }
  }

  private DynamicChecklist createSampleChecklist(String alertId) {
    List<ChecklistStep> steps =
        List.of(
            new ChecklistStep(
                1,
                "Check memory usage",
                "High memory may cause OOM killer",
                "92%",
                "Below 80%",
                StepPriority.HIGH,
                List.of("free -m")),
            new ChecklistStep(
                2,
                "Check processes",
                "Identify high memory processes",
                null,
                null,
                StepPriority.MEDIUM,
                List.of("top -o %MEM")));

    return new DynamicChecklist(
        alertId,
        "High memory usage troubleshooting guide",
        steps,
        List.of("runbooks/memory-troubleshooting.md"),
        Instant.now(),
        "ollama");
  }
}
