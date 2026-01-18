package com.oracle.runbook.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link OciConfig} configuration record. */
class OciConfigTest {

  // --- Environment Variable Parsing Tests (Task 1.1) ---

  @Nested
  @DisplayName("Environment variable parsing")
  class EnvironmentVariableTests {

    @Test
    @DisplayName("fromEnvironment returns valid config when all required env vars are set")
    void fromEnvironment_ReturnsConfig_WhenAllRequiredVarsSet() {
      Map<String, String> envVars = new HashMap<>();
      envVars.put("OCI_USER_ID", "ocid1.user.oc1..testuser");
      envVars.put("OCI_TENANCY_ID", "ocid1.tenancy.oc1..testtenancy");
      envVars.put("OCI_FINGERPRINT", "aa:bb:cc:dd:ee:ff");
      envVars.put("OCI_REGION", "us-ashburn-1");
      envVars.put(
          "OCI_PRIVATE_KEY_CONTENT",
          "-----BEGIN RSA PRIVATE KEY-----\ntest\n-----END RSA PRIVATE KEY-----");
      envVars.put("OCI_COMPARTMENT_ID", "ocid1.compartment.oc1..testcompartment");

      Optional<OciConfig> result = OciConfig.fromEnvironment(envVars::get);

      assertTrue(result.isPresent(), "Config should be present when all vars are set");
      OciConfig config = result.get();
      assertEquals("ocid1.compartment.oc1..testcompartment", config.compartmentId());
      assertEquals("us-ashburn-1", config.region());
      assertEquals("ocid1.user.oc1..testuser", config.userId());
      assertEquals("ocid1.tenancy.oc1..testtenancy", config.tenancyId());
      assertEquals("aa:bb:cc:dd:ee:ff", config.fingerprint());
      assertNotNull(config.privateKeyContent());
    }

    @Test
    @DisplayName("fromEnvironment returns empty when user ID is missing")
    void fromEnvironment_ReturnsEmpty_WhenUserIdMissing() {
      Map<String, String> envVars = new HashMap<>();
      envVars.put("OCI_TENANCY_ID", "ocid1.tenancy.oc1..testtenancy");
      envVars.put("OCI_FINGERPRINT", "aa:bb:cc:dd:ee:ff");
      envVars.put("OCI_REGION", "us-ashburn-1");
      envVars.put(
          "OCI_PRIVATE_KEY_CONTENT",
          "-----BEGIN RSA PRIVATE KEY-----\ntest\n-----END RSA PRIVATE KEY-----");
      envVars.put("OCI_COMPARTMENT_ID", "ocid1.compartment.oc1..testcompartment");

      Optional<OciConfig> result = OciConfig.fromEnvironment(envVars::get);

      assertTrue(result.isEmpty(), "Config should be empty when OCI_USER_ID is missing");
    }

    @Test
    @DisplayName("fromEnvironment returns empty when compartment ID is missing")
    void fromEnvironment_ReturnsEmpty_WhenCompartmentIdMissing() {
      Map<String, String> envVars = new HashMap<>();
      envVars.put("OCI_USER_ID", "ocid1.user.oc1..testuser");
      envVars.put("OCI_TENANCY_ID", "ocid1.tenancy.oc1..testtenancy");
      envVars.put("OCI_FINGERPRINT", "aa:bb:cc:dd:ee:ff");
      envVars.put("OCI_REGION", "us-ashburn-1");
      envVars.put(
          "OCI_PRIVATE_KEY_CONTENT",
          "-----BEGIN RSA PRIVATE KEY-----\ntest\n-----END RSA PRIVATE KEY-----");

      Optional<OciConfig> result = OciConfig.fromEnvironment(envVars::get);

      assertTrue(result.isEmpty(), "Config should be empty when OCI_COMPARTMENT_ID is missing");
    }

    @Test
    @DisplayName("fromEnvironment returns empty when all vars are empty")
    void fromEnvironment_ReturnsEmpty_WhenNoVarsSet() {
      Map<String, String> emptyEnv = Map.of();

      Optional<OciConfig> result = OciConfig.fromEnvironment(emptyEnv::get);

      assertTrue(result.isEmpty(), "Config should be empty when no env vars are set");
    }

    @Test
    @DisplayName("fromEnvironment uses OCI_PRIVATE_KEY_FILE when content not provided")
    void fromEnvironment_UsesKeyFile_WhenContentNotProvided() {
      Map<String, String> envVars = new HashMap<>();
      envVars.put("OCI_USER_ID", "ocid1.user.oc1..testuser");
      envVars.put("OCI_TENANCY_ID", "ocid1.tenancy.oc1..testtenancy");
      envVars.put("OCI_FINGERPRINT", "aa:bb:cc:dd:ee:ff");
      envVars.put("OCI_REGION", "us-ashburn-1");
      envVars.put("OCI_PRIVATE_KEY_FILE", "/path/to/key.pem");
      envVars.put("OCI_COMPARTMENT_ID", "ocid1.compartment.oc1..testcompartment");

      Optional<OciConfig> result = OciConfig.fromEnvironment(envVars::get);

      assertTrue(result.isPresent(), "Config should be present with key file path");
      assertEquals("/path/to/key.pem", result.get().privateKeyFilePath());
    }
  }

  @Test
  @DisplayName("OciConfig should hold all configuration fields")
  void shouldHoldConfigurationFields() {
    OciConfig config =
        new OciConfig(
            "ocid1.compartment.oc1..abc",
            "us-ashburn-1",
            "~/.oci/config",
            "DEFAULT",
            null,
            null,
            null,
            null,
            null);

    assertEquals("ocid1.compartment.oc1..abc", config.compartmentId());
    assertEquals("us-ashburn-1", config.region());
    assertEquals("~/.oci/config", config.configFilePath());
    assertEquals("DEFAULT", config.profile());
  }

  @Test
  @DisplayName("OciConfig should reject null compartmentId")
  void shouldRejectNullCompartmentId() {
    assertThrows(
        NullPointerException.class,
        () ->
            new OciConfig(
                null, "us-ashburn-1", "~/.oci/config", "DEFAULT", null, null, null, null, null));
  }

  @Test
  @DisplayName("OciConfig should allow null optional fields")
  void shouldAllowNullOptionalFields() {
    OciConfig config =
        new OciConfig(
            "ocid1.compartment.oc1..abc",
            null, // region optional
            null, // configFilePath optional
            null, // profile optional
            null, // userId optional
            null, // tenancyId optional
            null, // fingerprint optional
            null, // privateKeyContent optional
            null // privateKeyFilePath optional
            );

    assertEquals("ocid1.compartment.oc1..abc", config.compartmentId());
    assertNull(config.region());
    assertNull(config.configFilePath());
    assertNull(config.profile());
  }

  @Test
  @DisplayName("OciConfig should provide factory method with defaults")
  void shouldProvideFactoryWithDefaults() {
    OciConfig config = OciConfig.withDefaults("ocid1.compartment.oc1..abc");

    assertEquals("ocid1.compartment.oc1..abc", config.compartmentId());
    assertNull(config.region());
    assertEquals("~/.oci/config", config.configFilePath());
    assertEquals("DEFAULT", config.profile());
  }
}
