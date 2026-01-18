package com.oracle.runbook.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link OciAuthProviderFactory}. */
class OciAuthProviderFactoryTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("Should create auth provider from config file when exists")
  void shouldCreateAuthProviderFromConfigFile() throws IOException {
    // Setup: Create a mock OCI config file
    Path configFile = tempDir.resolve("config");
    String configContent =
        """
				[DEFAULT]
				user=ocid1.user.oc1..test
				fingerprint=aa:bb:cc:dd:ee:ff
				tenancy=ocid1.tenancy.oc1..test
				region=us-ashburn-1
				key_file=%s
				"""
            .formatted(createMockKeyFile(tempDir));
    Files.writeString(configFile, configContent);

    OciConfig config =
        new OciConfig(
            "ocid1.compartment.oc1..test",
            null,
            configFile.toString(),
            "DEFAULT",
            null,
            null,
            null,
            null,
            null);

    // Test: Factory should not throw for valid config
    assertThatCode(
            () -> {
              BasicAuthenticationDetailsProvider provider = OciAuthProviderFactory.create(config);
              assertThat(provider).isNotNull();
            })
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Should throw when config file does not exist")
  void shouldThrowWhenConfigFileNotExists() {
    OciConfig config =
        new OciConfig(
            "ocid1.compartment.oc1..test",
            null,
            "/nonexistent/path/config",
            "DEFAULT",
            null,
            null,
            null,
            null,
            null);

    assertThatThrownBy(() -> OciAuthProviderFactory.create(config))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Should throw when config is null")
  void shouldThrowWhenConfigIsNull() {
    assertThatThrownBy(() -> OciAuthProviderFactory.create(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("Should create auth provider from environment variables when set in config")
  void shouldCreateAuthProviderFromEnvironmentVariables() {
    // Create config with env-based auth fields populated
    OciConfig config =
        new OciConfig(
            "ocid1.compartment.oc1..test",
            "us-ashburn-1",
            null, // no config file
            null, // no profile
            "ocid1.user.oc1..testuser",
            "ocid1.tenancy.oc1..testtenancy",
            "aa:bb:cc:dd:ee:ff",
            "-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA0Z3VS5JJcds3xfn/ygWyF8PW3R7gHBACAQEwADANBgkqhkiG\n9w0BAQEFAAOCAg8AMIICCgKCAgEA0Z3VS5JJcds3xfn/ygWyF8PW3R7gHBACAgEA\n-----END RSA PRIVATE KEY-----",
            null);

    // Test: Factory should create SimpleAuthenticationDetailsProvider
    assertThatCode(
            () -> {
              BasicAuthenticationDetailsProvider provider = OciAuthProviderFactory.create(config);
              assertThat(provider).isNotNull();
            })
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Should prefer environment variables over config file when both present")
  void shouldPreferEnvVarsOverConfigFile() throws IOException {
    // Create valid config file
    Path configFile = tempDir.resolve("config");
    String configContent =
        """
				[DEFAULT]
				user=ocid1.user.oc1..fromconfigfile
				fingerprint=11:22:33:44:55:66
				tenancy=ocid1.tenancy.oc1..fromconfigfile
				region=us-phoenix-1
				key_file=%s
				"""
            .formatted(createMockKeyFile(tempDir));
    Files.writeString(configFile, configContent);

    // Config with BOTH env vars and config file - env vars should win
    OciConfig config =
        new OciConfig(
            "ocid1.compartment.oc1..test",
            "us-ashburn-1",
            configFile.toString(),
            "DEFAULT",
            "ocid1.user.oc1..fromenv", // env takes priority
            "ocid1.tenancy.oc1..fromenv",
            "aa:bb:cc:dd:ee:ff",
            "-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA0Z3VS5JJcds3xfn/ygWyF8PW3R7gHBACAQEwADANBgkqhkiG\n9w0BAQEFAAOCAg8AMIICCgKCAgEA0Z3VS5JJcds3xfn/ygWyF8PW3R7gHBACAgEA\n-----END RSA PRIVATE KEY-----",
            null);

    // Test: Factory should still succeed (but using env-based provider)
    BasicAuthenticationDetailsProvider provider = OciAuthProviderFactory.create(config);
    assertThat(provider).isNotNull();
    // The provider should have the env var user ID
    // Cast to AuthenticationDetailsProvider to access getUserId()
    AuthenticationDetailsProvider authProvider = (AuthenticationDetailsProvider) provider;
    assertThat(authProvider.getUserId())
        .as("Should use env var user ID, not config file")
        .contains("fromenv");
  }

  @Test
  @DisplayName("Should fall back to config file when environment variables not set")
  void shouldFallbackToConfigFileWhenEnvNotSet() throws IOException {
    // Create valid config file
    Path configFile = tempDir.resolve("config");
    String configContent =
        """
				[DEFAULT]
				user=ocid1.user.oc1..fromconfigfile
				fingerprint=11:22:33:44:55:66
				tenancy=ocid1.tenancy.oc1..fromconfigfile
				region=us-phoenix-1
				key_file=%s
				"""
            .formatted(createMockKeyFile(tempDir));
    Files.writeString(configFile, configContent);

    // Config with only config file, no env vars
    OciConfig config =
        new OciConfig(
            "ocid1.compartment.oc1..test",
            null,
            configFile.toString(),
            "DEFAULT",
            null, // no userId
            null,
            null,
            null,
            null);

    BasicAuthenticationDetailsProvider provider = OciAuthProviderFactory.create(config);
    assertThat(provider).isNotNull();
    // Cast to AuthenticationDetailsProvider to access getUserId()
    AuthenticationDetailsProvider authProvider = (AuthenticationDetailsProvider) provider;
    assertThat(authProvider.getUserId())
        .as("Should fallback to config file user ID")
        .contains("fromconfigfile");
  }

  /** Creates a mock RSA private key file for testing. */
  private String createMockKeyFile(Path dir) throws IOException {
    Path keyFile = dir.resolve("oci_api_key.pem");
    // Minimal PEM format (not a real key, just for parsing test)
    String keyContent =
        """
				-----BEGIN RSA PRIVATE KEY-----
				MIIEpAIBAAKCAQEA0Z3VS5JJcds3xfn/ygWyF8PW3R7gHBACAQEwADANBgkqhkiG
				9w0BAQEFAAOCAg8AMIICCgKCAgEA0Z3VS5JJcds3xfn/ygWyF8PW3R7gHBACAgEA
				-----END RSA PRIVATE KEY-----
				""";
    Files.writeString(keyFile, keyContent);
    return keyFile.toString();
  }
}
