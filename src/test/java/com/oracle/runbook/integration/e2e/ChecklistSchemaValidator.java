package com.oracle.runbook.integration.e2e;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates generated checklist JSON files against expected schema.
 *
 * <p>Provides methods to:
 *
 * <ul>
 *   <li>Validate complete schema conformance
 *   <li>Check for expected required fields
 *   <li>Find missing fields
 *   <li>Validate step ordering
 * </ul>
 */
public final class ChecklistSchemaValidator {

  private static final List<String> REQUIRED_FIELDS =
      List.of("alertId", "summary", "steps", "sourceRunbooks", "generatedAt", "llmProviderUsed");

  private ChecklistSchemaValidator() {
    // Utility class
  }

  /**
   * Validates a checklist file against the expected schema.
   *
   * @param path path to the checklist JSON file
   * @return validation result with success status and any errors
   */
  public static ValidationResult validate(Path path) {
    List<String> errors = new ArrayList<>();

    try {
      String content = Files.readString(path);
      JsonObject json = parseJson(content);

      // Check required fields
      List<String> missing = findMissingFieldsFromJson(json);
      if (!missing.isEmpty()) {
        errors.add("Missing required fields: " + missing);
      }

      // Validate alertId is non-empty
      if (json.containsKey("alertId")) {
        String alertId = json.getString("alertId", "");
        if (alertId.isBlank()) {
          errors.add("alertId cannot be blank");
        }
      }

      // Validate steps array
      if (json.containsKey("steps")) {
        JsonArray steps = json.getJsonArray("steps");
        if (steps.isEmpty()) {
          errors.add("steps array cannot be empty");
        }

        // Validate each step
        for (int i = 0; i < steps.size(); i++) {
          JsonObject step = steps.getJsonObject(i);
          List<String> stepErrors = validateStep(step, i);
          errors.addAll(stepErrors);
        }

        // Validate ordering
        if (!validateStepOrderingFromJson(json)) {
          errors.add("steps are not in sequential order");
        }
      }

      // Validate sourceRunbooks is non-empty
      if (json.containsKey("sourceRunbooks")) {
        JsonArray runbooks = json.getJsonArray("sourceRunbooks");
        if (runbooks.isEmpty()) {
          errors.add("sourceRunbooks array cannot be empty");
        }
      }

      // Validate llmProviderUsed is non-blank
      if (json.containsKey("llmProviderUsed")) {
        String provider = json.getString("llmProviderUsed", "");
        if (provider.isBlank()) {
          errors.add("llmProviderUsed cannot be blank");
        }
      }

    } catch (Exception e) {
      errors.add("Failed to parse JSON: " + e.getMessage());
    }

    return new ValidationResult(errors.isEmpty(), errors);
  }

  /**
   * Checks if a checklist file has all expected fields.
   *
   * @param path path to the checklist JSON file
   * @return true if all required fields are present
   */
  public static boolean hasExpectedFields(Path path) {
    try {
      String content = Files.readString(path);
      JsonObject json = parseJson(content);
      return findMissingFieldsFromJson(json).isEmpty();
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Returns list of missing required fields from a checklist file.
   *
   * @param path path to the checklist JSON file
   * @return list of missing field names (empty if all present)
   */
  public static List<String> findMissingFields(Path path) {
    try {
      String content = Files.readString(path);
      JsonObject json = parseJson(content);
      return findMissingFieldsFromJson(json);
    } catch (Exception e) {
      return List.of("Unable to parse file: " + e.getMessage());
    }
  }

  /**
   * Validates that steps are in sequential order (1, 2, 3...).
   *
   * @param path path to the checklist JSON file
   * @return true if steps are sequentially ordered
   */
  public static boolean validateStepOrdering(Path path) {
    try {
      String content = Files.readString(path);
      JsonObject json = parseJson(content);
      return validateStepOrderingFromJson(json);
    } catch (Exception e) {
      return false;
    }
  }

  // ========== Private Helpers ==========

  private static List<String> findMissingFieldsFromJson(JsonObject json) {
    List<String> missing = new ArrayList<>();
    for (String field : REQUIRED_FIELDS) {
      if (!json.containsKey(field)) {
        missing.add(field);
      }
    }
    return missing;
  }

  private static boolean validateStepOrderingFromJson(JsonObject json) {
    if (!json.containsKey("steps")) {
      return false;
    }
    JsonArray steps = json.getJsonArray("steps");
    for (int i = 0; i < steps.size(); i++) {
      JsonObject step = steps.getJsonObject(i);
      int expectedOrder = i + 1;
      int actualOrder = step.getInt("order", -1);
      if (actualOrder != expectedOrder) {
        return false;
      }
    }
    return true;
  }

  private static List<String> validateStep(JsonObject step, int index) {
    List<String> errors = new ArrayList<>();

    // Check required step fields
    if (!step.containsKey("order")) {
      errors.add("Step " + index + " is missing 'order' field");
    }
    if (!step.containsKey("instruction")) {
      errors.add("Step " + index + " is missing 'instruction' field");
    } else {
      String instruction = step.getString("instruction", "");
      if (instruction.isBlank()) {
        errors.add("Step " + index + " instruction cannot be blank");
      }
    }

    return errors;
  }

  private static JsonObject parseJson(String json) {
    try (JsonReader reader = Json.createReader(new StringReader(json))) {
      return reader.readObject();
    }
  }

  /** Result of schema validation. */
  public record ValidationResult(boolean isValid, List<String> errors) {
    public ValidationResult {
      errors = errors != null ? List.copyOf(errors) : List.of();
    }
  }
}
