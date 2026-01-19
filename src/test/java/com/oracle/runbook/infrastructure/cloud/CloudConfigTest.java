package com.oracle.runbook.infrastructure.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for {@link CloudConfig} interface.
 *
 * <p>Verifies the interface contract that all cloud provider configurations must implement.
 */
class CloudConfigTest {

  @Test
  @DisplayName("CloudConfig should be an interface")
  void cloudConfigShouldBeInterface() {
    assertThat(CloudConfig.class.isInterface())
        .as("CloudConfig must be an interface for polymorphic cloud configuration")
        .isTrue();
  }

  @Test
  @DisplayName("CloudConfig should declare provider() method")
  void shouldDeclareProviderMethod() throws NoSuchMethodException {
    var method = CloudConfig.class.getMethod("provider");

    assertThat(method.getReturnType())
        .as("provider() should return String")
        .isEqualTo(String.class);
    assertThat(method.getParameterCount()).as("provider() should take no parameters").isZero();
  }

  @Test
  @DisplayName("CloudConfig should declare region() method")
  void shouldDeclareRegionMethod() throws NoSuchMethodException {
    var method = CloudConfig.class.getMethod("region");

    assertThat(method.getReturnType()).as("region() should return String").isEqualTo(String.class);
    assertThat(method.getParameterCount()).as("region() should take no parameters").isZero();
  }
}
