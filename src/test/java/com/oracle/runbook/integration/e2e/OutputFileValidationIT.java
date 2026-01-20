package com.oracle.runbook.integration.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.oracle.runbook.domain.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;

/**
 * Integration tests that validate generated checklist output files.
 *
 * <p>Ensures generated checklist files are valid and actionable:
 *
 * <ul>
 *   <li>Valid JSON schema conformance
 *   <li>Filename format compliance
 *   <li>Required fields present and valid
 *   <li>Step ordering and content validation
 * </ul>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OutputFileValidationIT {

  private static Path outputDir;
  private static PipelineTestHarness harness;
  private static DynamicChecklist generatedChecklist;
  private static Path generatedFile;

  @BeforeAll
  static void setupAndGenerateChecklist() throws Exception {
    // Create output directory
    outputDir = Files.createTempDirectory("output-validation");

    // Create harness
    harness = PipelineTestHarness.testMode().withOutputDirectory(outputDir).withTimeout(30).build();

    // Seed runbooks for content validation
    harness.seedRunbooks(
        "sample-runbooks/memory-troubleshooting.md", "sample-runbooks/cpu-troubleshooting.md");

    // Generate a checklist for validation tests
    Alert testAlert =
        new Alert(
            "alert-validation-001",
            "High Memory Alert for Validation",
            "Memory utilization exceeded threshold for validation testing",
            AlertSeverity.WARNING,
            "cloudwatch",
            Map.of("instanceId", "i-validation12345"),
            Map.of("alarmName", "ValidationTestAlarm"),
            Instant.now(),
            "{}");

    generatedChecklist = harness.processAlert(testAlert);
    generatedFile = harness.getOutputFile("alert-validation-001");
  }

  @AfterAll
  static void cleanup() throws Exception {
    if (outputDir != null) {
      Files.walk(outputDir)
          .sorted((a, b) -> -a.compareTo(b))
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
              });
    }
  }

  // ========== JSON Schema Validation Tests ==========

  @Test
  @Order(1)
  @DisplayName("Should output valid JSON schema")
  void shouldOutputValidJsonSchema() throws Exception {
    // Given: Generated output file
    assertThat(generatedFile).isNotNull();
    assertThat(Files.exists(generatedFile)).isTrue();

    // When: Validate against schema
    ChecklistSchemaValidator.ValidationResult result =
        ChecklistSchemaValidator.validate(generatedFile);

    // Then: Schema validation passes
    assertThat(result.isValid()).as("JSON schema validation failed: %s", result.errors()).isTrue();
    assertThat(result.errors()).isEmpty();
  }

  // ========== Filename Format Tests ==========

  @Test
  @Order(2)
  @DisplayName("Should contain alert ID in filename")
  void shouldContainAlertIdInFilename() {
    // Given: Generated output file
    assertThat(generatedFile).isNotNull();

    // When: Extract filename
    String filename = generatedFile.getFileName().toString();

    // Then: Filename contains alert ID
    assertThat(filename).contains("alert-validation-001");
  }

  @Test
  @Order(3)
  @DisplayName("Should contain timestamp in filename")
  void shouldContainTimestampInFilename() {
    // Given: Generated output file
    assertThat(generatedFile).isNotNull();

    // When: Extract filename
    String filename = generatedFile.getFileName().toString();

    // Then: Filename matches pattern checklist-{alertId}-{timestamp}.json
    // Timestamp format: ISO instant or milliseconds
    Pattern filenamePattern = Pattern.compile("checklist-[\\w-]+-\\d+\\.json");
    assertThat(filename).matches(filenamePattern.pattern());
  }

  // ========== Content Validation Tests ==========

  @Test
  @Order(4)
  @DisplayName("Should have non-empty steps")
  void shouldHaveNonEmptySteps() {
    // Then: Checklist has steps
    assertThat(generatedChecklist.steps()).isNotEmpty();
  }

  @Test
  @Order(5)
  @DisplayName("Should have valid step ordering")
  void shouldHaveValidStepOrdering() {
    // Given: Steps from checklist
    List<ChecklistStep> steps = generatedChecklist.steps();

    // Then: Steps have sequential order numbers (1, 2, 3...)
    for (int i = 0; i < steps.size(); i++) {
      assertThat(steps.get(i).order())
          .as("Step %d should have order %d", i, i + 1)
          .isEqualTo(i + 1);
    }
  }

  @Test
  @Order(6)
  @DisplayName("Should include source runbooks")
  void shouldIncludeSourceRunbooks() {
    // Then: Source runbooks list is populated
    assertThat(generatedChecklist.sourceRunbooks()).isNotEmpty();
    assertThat(generatedChecklist.sourceRunbooks())
        .anyMatch(runbook -> runbook.contains("troubleshooting"));
  }

  @Test
  @Order(7)
  @DisplayName("Should include LLM provider used")
  void shouldIncludeLlmProviderUsed() {
    // Then: LLM provider field is set
    assertThat(generatedChecklist.llmProviderUsed()).isNotBlank();
  }

  @Test
  @Order(8)
  @DisplayName("Should have commands field accessible in steps")
  void shouldHaveCommandsInSteps() {
    // Given: Steps from checklist
    List<ChecklistStep> steps = generatedChecklist.steps();

    // Then: Every step has a commands() field (may be empty in test mode)
    // This verifies the field exists and is an array - actual command presence depends on LLM
    assertThat(steps).isNotEmpty();
    assertThat(steps)
        .allSatisfy(
            step -> {
              // commands() should never return null due to defensive copy in record
              assertThat(step.commands()).isNotNull();
            });

    // Note: In production with real LLM, we'd expect at least some steps to have commands
    // Test mode LLM doesn't generate commands, so we just verify the structure is valid
    long stepsWithCommands = steps.stream().filter(step -> !step.commands().isEmpty()).count();
    // Log the count for debugging purposes (no assertion failure if zero in test mode)
    System.out.println("Steps with commands: " + stepsWithCommands + " of " + steps.size());
  }

  @Test
  @Order(9)
  @DisplayName("Should reference seeded content")
  void shouldReferenceSeededContent() {
    // Given: Generated checklist
    assertThat(generatedChecklist).isNotNull();

    // When: Extract all instructions
    List<String> instructions =
        generatedChecklist.steps().stream().map(ChecklistStep::instruction).toList();
    String allInstructions = String.join(" ", instructions).toLowerCase();

    // Then: Instructions reference memory-related content (from seeded runbook)
    assertThat(allInstructions).containsAnyOf("memory", "free", "top", "check");
  }

  @Test
  @Order(10)
  @DisplayName("Should be readable by external tools (valid JSON)")
  void shouldBeReadableByExternalTools() throws Exception {
    // Given: Generated output file
    assertThat(generatedFile).isNotNull();

    // When: Read and parse the file content
    String content = Files.readString(generatedFile);

    // Then: Content is valid JSON (can be parsed via Jackson or JSON-P)
    assertThat(content).startsWith("{");
    assertThat(content).contains("\"alertId\"");
    assertThat(content).contains("\"steps\"");
    assertThat(content).contains("\"sourceRunbooks\"");
    assertThat(content).contains("\"generatedAt\"");
    assertThat(content).contains("\"llmProviderUsed\"");

    // And: Validate with ChecklistSchemaValidator
    boolean hasExpectedFields = ChecklistSchemaValidator.hasExpectedFields(generatedFile);
    assertThat(hasExpectedFields).isTrue();
  }

  // ========== Additional Validation Tests ==========

  @Test
  @Order(11)
  @DisplayName("Should validate step ordering via ChecklistSchemaValidator")
  void shouldValidateStepOrderingViaValidator() throws Exception {
    // When: Validate step ordering
    boolean validOrdering = ChecklistSchemaValidator.validateStepOrdering(generatedFile);

    // Then: Ordering is valid
    assertThat(validOrdering).isTrue();
  }

  @Test
  @Order(12)
  @DisplayName("Should find no missing required fields")
  void shouldFindNoMissingRequiredFields() throws Exception {
    // When: Check for missing fields
    List<String> missingFields = ChecklistSchemaValidator.findMissingFields(generatedFile);

    // Then: No fields are missing
    assertThat(missingFields).isEmpty();
  }

  @Test
  @Order(13)
  @DisplayName("Alert ID in file should match triggering alert")
  void shouldMatchTriggeringAlertId() {
    // Then: Alert ID in checklist matches the alert we sent
    assertThat(generatedChecklist.alertId()).isEqualTo("alert-validation-001");
  }
}
