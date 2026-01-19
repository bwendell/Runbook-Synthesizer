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
      AwsConfig config = new AwsConfig("us-east-1", "test-bucket", null, null);

      assertThat(config)
          .as("AwsConfig must implement CloudConfig for polymorphic cloud configuration")
          .isInstanceOf(CloudConfig.class);
    }

    @Test
    @DisplayName("provider() should return 'aws'")
    void providerShouldReturnAws() {
      AwsConfig config = new AwsConfig("us-east-1", "test-bucket", null, null);

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
      assertThatThrownBy(() -> new AwsConfig("us-east-1", null, null, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("bucket");
    }

    @Test
    @DisplayName("AwsConfig should allow null accessKeyId for default credential chain")
    void shouldAllowNullAccessKeyId() {
      AwsConfig config = new AwsConfig("us-east-1", "test-bucket", null, null);

      assertThat(config.accessKeyId()).isNull();
    }

    @Test
    @DisplayName("AwsConfig should allow null secretAccessKey for default credential chain")
    void shouldAllowNullSecretAccessKey() {
      AwsConfig config = new AwsConfig("us-east-1", "test-bucket", "AKIAIOSFODNN7EXAMPLE", null);

      assertThat(config.secretAccessKey()).isNull();
    }
  }

  @Nested
  @DisplayName("Field accessors")
  class FieldAccessorTests {

    @Test
    @DisplayName("bucket() should return configured bucket")
    void bucketShouldReturnConfiguredBucket() {
      AwsConfig config = new AwsConfig("us-east-1", "my-runbooks-bucket", null, null);

      assertThat(config.bucket()).isEqualTo("my-runbooks-bucket");
    }

    @Test
    @DisplayName("accessKeyId() should return configured access key")
    void accessKeyIdShouldReturnConfiguredValue() {
      AwsConfig config =
          new AwsConfig("us-east-1", "test-bucket", "AKIAIOSFODNN7EXAMPLE", "secretkey123");

      assertThat(config.accessKeyId()).isEqualTo("AKIAIOSFODNN7EXAMPLE");
    }
  }
}
