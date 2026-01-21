package com.oracle.runbook.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RunbookConfig}.
 *
 * <p>Tests follow TDD red-green-refactor cycle. These tests will initially fail until RunbookConfig
 * is implemented.
 */
class RunbookConfigTest {

  @Nested
  @DisplayName("Default Values Tests")
  class DefaultValuesTests {

    @Test
    @DisplayName("Should load default bucket name when not configured")
    void shouldLoadDefaultBucketName_WhenNotConfigured() {
      Config config = Config.builder().sources(ConfigSources.create(Map.of())).build();

      RunbookConfig runbookConfig = new RunbookConfig(config);

      assertThat(runbookConfig.bucket()).isEqualTo("runbook-synthesizer-runbooks");
    }

    @Test
    @DisplayName("Should load default ingestOnStartup as true when not configured")
    void shouldLoadDefaultIngestOnStartup_WhenNotConfigured() {
      Config config = Config.builder().sources(ConfigSources.create(Map.of())).build();

      RunbookConfig runbookConfig = new RunbookConfig(config);

      assertThat(runbookConfig.ingestOnStartup()).isTrue();
    }
  }

  @Nested
  @DisplayName("Custom Configuration Tests")
  class CustomConfigurationTests {

    @Test
    @DisplayName("Should load custom bucket name when configured")
    void shouldLoadCustomBucketName_WhenConfigured() {
      Config config =
          Config.builder()
              .sources(ConfigSources.create(Map.of("runbooks.bucket", "my-custom-bucket")))
              .build();

      RunbookConfig runbookConfig = new RunbookConfig(config);

      assertThat(runbookConfig.bucket()).isEqualTo("my-custom-bucket");
    }

    @Test
    @DisplayName("Should load ingestOnStartup as false when configured")
    void shouldLoadIngestOnStartup_False_WhenConfigured() {
      Config config =
          Config.builder()
              .sources(ConfigSources.create(Map.of("runbooks.ingestOnStartup", "false")))
              .build();

      RunbookConfig runbookConfig = new RunbookConfig(config);

      assertThat(runbookConfig.ingestOnStartup()).isFalse();
    }
  }

  @Nested
  @DisplayName("Record Accessor Tests")
  class RecordAccessorTests {

    @Test
    @DisplayName("Should provide both bucket and ingestOnStartup accessors")
    void shouldProvideBothAccessors() {
      Config config =
          Config.builder()
              .sources(
                  ConfigSources.create(
                      Map.of(
                          "runbooks.bucket", "test-bucket",
                          "runbooks.ingestOnStartup", "true")))
              .build();

      RunbookConfig runbookConfig = new RunbookConfig(config);

      assertThat(runbookConfig.bucket()).isEqualTo("test-bucket");
      assertThat(runbookConfig.ingestOnStartup()).isTrue();
    }
  }
}
