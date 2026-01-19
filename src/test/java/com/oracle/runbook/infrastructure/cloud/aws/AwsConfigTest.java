package com.oracle.runbook.infrastructure.cloud.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.runbook.infrastructure.cloud.CloudConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AwsConfig} configuration record.
 *
 * <p>Tests follow OciConfigTest structure per testing-patterns-java skill.
 */
class AwsConfigTest {

  @Nested
  @DisplayName("CloudConfig interface implementation")
  class CloudConfigImplementationTests {

    @Test
    @DisplayName("AwsConfig should implement CloudConfig interface")
    void shouldImplementCloudConfig() {
      AwsConfig config = new AwsConfig("us-west-2", "test-bucket", null, null);

      assertThat(config)
          .as("AwsConfig must implement CloudConfig for polymorphic cloud configuration")
          .isInstanceOf(CloudConfig.class);
    }

    @Test
    @DisplayName("provider() should return 'aws'")
    void providerShouldReturnAws() {
      AwsConfig config = new AwsConfig("us-west-2", "test-bucket", null, null);

      assertThat(config.provider())
          .as("provider() must return 'aws' for AWS configuration")
          .isEqualTo("aws");
    }

    @Test
    @DisplayName("region() should return configured region")
    void regionShouldReturnConfiguredRegion() {
      AwsConfig config = new AwsConfig("us-west-2", "test-bucket", null, null);

      assertThat(config.region())
          .as("region() must return the configured region")
          .isEqualTo("us-west-2");
    }
  }

  @Nested
  @DisplayName("Field validation")
  class FieldValidationTests {

    @Test
    @DisplayName("AwsConfig should reject null region")
    void shouldRejectNullRegion() {
      assertThatThrownBy(() -> new AwsConfig(null, "test-bucket", null, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("region");
    }

    @Test
    @DisplayName("AwsConfig should reject null bucket")
    void shouldRejectNullBucket() {
      assertThatThrownBy(() -> new AwsConfig("us-west-2", null, null, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("bucket");
    }

    @Test
    @DisplayName("AwsConfig should allow null accessKeyId for default credential chain")
    void shouldAllowNullAccessKeyId() {
      AwsConfig config = new AwsConfig("us-west-2", "test-bucket", null, null);

      assertThat(config.accessKeyId()).isNull();
    }

    @Test
    @DisplayName("AwsConfig should allow null secretAccessKey for default credential chain")
    void shouldAllowNullSecretAccessKey() {
      AwsConfig config = new AwsConfig("us-west-2", "test-bucket", "AKIAIOSFODNN7EXAMPLE", null);

      assertThat(config.secretAccessKey()).isNull();
    }
  }

  @Nested
  @DisplayName("Field accessors")
  class FieldAccessorTests {

    @Test
    @DisplayName("bucket() should return configured bucket")
    void bucketShouldReturnConfiguredBucket() {
      AwsConfig config = new AwsConfig("us-west-2", "my-runbooks-bucket", null, null);

      assertThat(config.bucket()).isEqualTo("my-runbooks-bucket");
    }

    @Test
    @DisplayName("accessKeyId() should return configured access key")
    void accessKeyIdShouldReturnConfiguredValue() {
      AwsConfig config =
          new AwsConfig("us-west-2", "test-bucket", "AKIAIOSFODNN7EXAMPLE", "secretkey123");

      assertThat(config.accessKeyId()).isEqualTo("AKIAIOSFODNN7EXAMPLE");
    }
  }

  @Nested
  @DisplayName("Environment variable parsing")
  class EnvironmentVariableTests {

    @Test
    @DisplayName("fromEnvironment returns valid config when all required env vars are set")
    void fromEnvironment_ReturnsConfig_WhenAllRequiredVarsSet() {
      java.util.Map<String, String> envVars = new java.util.HashMap<>();
      envVars.put("AWS_REGION", "us-west-2");
      envVars.put("AWS_S3_BUCKET", "test-runbooks");
      envVars.put("AWS_ACCESS_KEY_ID", "AKIAIOSFODNN7EXAMPLE");
      envVars.put("AWS_SECRET_ACCESS_KEY", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

      java.util.Optional<AwsConfig> result = AwsConfig.fromEnvironment(envVars::get);

      assertThat(result).as("Config should be present when all vars are set").isPresent();
      AwsConfig config = result.get();
      assertThat(config.region()).isEqualTo("us-west-2");
      assertThat(config.bucket()).isEqualTo("test-runbooks");
      assertThat(config.accessKeyId()).isEqualTo("AKIAIOSFODNN7EXAMPLE");
      assertThat(config.secretAccessKey()).isEqualTo("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
    }

    @Test
    @DisplayName(
        "fromEnvironment returns config with minimal required env vars (uses default credential chain)")
    void fromEnvironment_ReturnsConfig_WithMinimalRequiredVars() {
      java.util.Map<String, String> envVars = new java.util.HashMap<>();
      envVars.put("AWS_REGION", "eu-west-1");
      envVars.put("AWS_S3_BUCKET", "my-bucket");

      java.util.Optional<AwsConfig> result = AwsConfig.fromEnvironment(envVars::get);

      assertThat(result).as("Config should be present with just region and bucket").isPresent();
      AwsConfig config = result.get();
      assertThat(config.region()).isEqualTo("eu-west-1");
      assertThat(config.bucket()).isEqualTo("my-bucket");
      assertThat(config.accessKeyId()).as("Should use default credential chain").isNull();
      assertThat(config.secretAccessKey()).as("Should use default credential chain").isNull();
    }

    @Test
    @DisplayName("fromEnvironment returns empty when region is missing")
    void fromEnvironment_ReturnsEmpty_WhenRegionMissing() {
      java.util.Map<String, String> envVars = new java.util.HashMap<>();
      envVars.put("AWS_S3_BUCKET", "test-bucket");

      java.util.Optional<AwsConfig> result = AwsConfig.fromEnvironment(envVars::get);

      assertThat(result).as("Config should be empty when AWS_REGION is missing").isEmpty();
    }

    @Test
    @DisplayName("fromEnvironment returns empty when bucket is missing")
    void fromEnvironment_ReturnsEmpty_WhenBucketMissing() {
      java.util.Map<String, String> envVars = new java.util.HashMap<>();
      envVars.put("AWS_REGION", "us-west-2");

      java.util.Optional<AwsConfig> result = AwsConfig.fromEnvironment(envVars::get);

      assertThat(result).as("Config should be empty when AWS_S3_BUCKET is missing").isEmpty();
    }

    @Test
    @DisplayName("fromEnvironment returns empty when no vars are set")
    void fromEnvironment_ReturnsEmpty_WhenNoVarsSet() {
      java.util.Map<String, String> emptyEnv = java.util.Map.of();

      java.util.Optional<AwsConfig> result = AwsConfig.fromEnvironment(emptyEnv::get);

      assertThat(result).as("Config should be empty when no env vars are set").isEmpty();
    }
  }
}
