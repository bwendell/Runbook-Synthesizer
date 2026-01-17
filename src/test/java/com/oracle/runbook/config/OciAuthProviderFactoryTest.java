package com.oracle.runbook.config;

import static org.junit.jupiter.api.Assertions.*;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
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
        new OciConfig("ocid1.compartment.oc1..test", null, configFile.toString(), "DEFAULT");

    // Test: Factory should not throw for valid config
    assertDoesNotThrow(
        () -> {
          AuthenticationDetailsProvider provider = OciAuthProviderFactory.create(config);
          assertNotNull(provider);
        });
  }

  @Test
  @DisplayName("Should throw when config file does not exist")
  void shouldThrowWhenConfigFileNotExists() {
    OciConfig config =
        new OciConfig("ocid1.compartment.oc1..test", null, "/nonexistent/path/config", "DEFAULT");

    assertThrows(IllegalStateException.class, () -> OciAuthProviderFactory.create(config));
  }

  @Test
  @DisplayName("Should throw when config is null")
  void shouldThrowWhenConfigIsNull() {
    assertThrows(NullPointerException.class, () -> OciAuthProviderFactory.create(null));
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
