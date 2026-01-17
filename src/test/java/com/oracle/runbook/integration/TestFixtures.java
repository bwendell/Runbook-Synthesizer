package com.oracle.runbook.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for loading test fixtures from resources.
 *
 * <p>Loads JSON files from {@code src/test/resources/fixtures/} directory.
 */
public final class TestFixtures {

  private static final String FIXTURES_BASE_PATH = "fixtures/";
  private static final ObjectMapper OBJECT_MAPPER;

  static {
    OBJECT_MAPPER = new ObjectMapper();
    OBJECT_MAPPER.registerModule(new JavaTimeModule());
  }

  private TestFixtures() {}

  /**
   * Loads a JSON file from the fixtures directory.
   *
   * @param path relative path within fixtures directory (e.g., "alerts/high-memory-alert.json")
   * @return parsed JsonObject
   * @throws IllegalArgumentException if file not found or invalid JSON
   */
  public static JsonObject loadJson(String path) {
    String content = loadString(path);
    try (JsonReader reader = Json.createReader(new StringReader(content))) {
      return reader.readObject();
    }
  }

  /**
   * Loads a JSON file and deserializes to the specified type.
   *
   * @param path relative path within fixtures directory
   * @param type class to deserialize to
   * @param <T> target type
   * @return deserialized object
   * @throws IllegalArgumentException if file not found or deserialization fails
   */
  public static <T> T loadAs(String path, Class<T> type) {
    String content = loadString(path);
    try {
      return OBJECT_MAPPER.readValue(content, type);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to deserialize fixture: " + path, e);
    }
  }

  /**
   * Loads a fixture file as a raw string.
   *
   * @param path relative path within fixtures directory
   * @return file content as string
   * @throws IllegalArgumentException if file not found
   */
  public static String loadString(String path) {
    String fullPath = FIXTURES_BASE_PATH + path;
    try (InputStream is = TestFixtures.class.getClassLoader().getResourceAsStream(fullPath)) {
      if (is == null) {
        throw new IllegalArgumentException("Fixture file not found: " + fullPath);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to read fixture: " + fullPath, e);
    }
  }
}
