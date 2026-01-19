package com.oracle.runbook.infrastructure.cloud.oci;

import com.oracle.runbook.infrastructure.cloud.CloudConfig;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Configuration record for OCI authentication and connectivity settings.
 *
 * <p>Holds the necessary configuration for connecting to OCI services. Supports two authentication
 * modes:
 *
 * <ol>
 *   <li><strong>Config file mode</strong>: Uses ~/.oci/config with profile selection (traditional
 *       OCI CLI setup)
 *   <li><strong>Environment variable mode</strong>: Uses OCI_* environment variables (CI/CD
 *       friendly)
 * </ol>
 *
 * @param compartmentId the OCI compartment OCID (required)
 * @param region the OCI region identifier (optional, e.g., "us-ashburn-1")
 * @param configFilePath the path to OCI config file (optional, defaults to ~/.oci/config)
 * @param profile the OCI config profile name (optional, defaults to "DEFAULT")
 * @param userId the OCI user OCID for env var auth (optional)
 * @param tenancyId the OCI tenancy OCID for env var auth (optional)
 * @param fingerprint the API key fingerprint for env var auth (optional)
 * @param privateKeyContent the PEM private key content for env var auth (optional)
 * @param privateKeyFilePath the path to PEM private key file for env var auth (optional)
 */
public record OciConfig(
    String compartmentId,
    String region,
    String configFilePath,
    String profile,
    String userId,
    String tenancyId,
    String fingerprint,
    String privateKeyContent,
    String privateKeyFilePath)
    implements CloudConfig {
  /** Environment variable names for OCI authentication. */
  public static final String ENV_USER_ID = "OCI_USER_ID";

  public static final String ENV_TENANCY_ID = "OCI_TENANCY_ID";
  public static final String ENV_FINGERPRINT = "OCI_FINGERPRINT";
  public static final String ENV_REGION = "OCI_REGION";
  public static final String ENV_PRIVATE_KEY_CONTENT = "OCI_PRIVATE_KEY_CONTENT";
  public static final String ENV_PRIVATE_KEY_FILE = "OCI_PRIVATE_KEY_FILE";
  public static final String ENV_COMPARTMENT_ID = "OCI_COMPARTMENT_ID";

  /** Compact constructor with validation. */
  public OciConfig {
    Objects.requireNonNull(compartmentId, "OciConfig compartmentId cannot be null");
  }

  /**
   * Returns the cloud provider identifier.
   *
   * @return "oci" always
   */
  @Override
  public String provider() {
    return "oci";
  }

  /**
   * Factory method that creates an OciConfig with sensible defaults.
   *
   * <p>Uses ~/.oci/config as the config file path and "DEFAULT" as the profile.
   *
   * @param compartmentId the OCI compartment OCID (required)
   * @return a new OciConfig instance with default values for optional fields
   */
  public static OciConfig withDefaults(String compartmentId) {
    return new OciConfig(
        compartmentId, null, "~/.oci/config", "DEFAULT", null, null, null, null, null);
  }

  /**
   * Creates an OciConfig from environment variables if all required variables are present.
   *
   * <p>Requires the following environment variables:
   *
   * <ul>
   *   <li>{@code OCI_USER_ID} - OCI user OCID
   *   <li>{@code OCI_TENANCY_ID} - OCI tenancy OCID
   *   <li>{@code OCI_FINGERPRINT} - API key fingerprint
   *   <li>{@code OCI_REGION} - OCI region
   *   <li>{@code OCI_COMPARTMENT_ID} - OCI compartment OCID
   *   <li>{@code OCI_PRIVATE_KEY_CONTENT} or {@code OCI_PRIVATE_KEY_FILE} - Private key
   * </ul>
   *
   * @param envLookup function to look up environment variable values (e.g., System::getenv)
   * @return Optional containing OciConfig if all required vars present, empty otherwise
   */
  public static Optional<OciConfig> fromEnvironment(Function<String, String> envLookup) {
    String userId = envLookup.apply(ENV_USER_ID);
    String tenancyId = envLookup.apply(ENV_TENANCY_ID);
    String fingerprint = envLookup.apply(ENV_FINGERPRINT);
    String region = envLookup.apply(ENV_REGION);
    String compartmentId = envLookup.apply(ENV_COMPARTMENT_ID);
    String privateKeyContent = envLookup.apply(ENV_PRIVATE_KEY_CONTENT);
    String privateKeyFilePath = envLookup.apply(ENV_PRIVATE_KEY_FILE);

    // All required fields must be present
    if (isBlank(userId) || isBlank(tenancyId) || isBlank(fingerprint) || isBlank(compartmentId)) {
      return Optional.empty();
    }

    // Need either key content or key file path
    if (isBlank(privateKeyContent) && isBlank(privateKeyFilePath)) {
      return Optional.empty();
    }

    return Optional.of(
        new OciConfig(
            compartmentId,
            region,
            null, // configFilePath not used in env mode
            null, // profile not used in env mode
            userId,
            tenancyId,
            fingerprint,
            privateKeyContent,
            privateKeyFilePath));
  }

  /**
   * Creates an OciConfig from system environment variables.
   *
   * @return Optional containing OciConfig if all required vars present, empty otherwise
   */
  public static Optional<OciConfig> fromEnvironment() {
    return fromEnvironment(System::getenv);
  }

  /**
   * Returns true if this config was created from environment variables.
   *
   * @return true if userId is set (indicating env var mode)
   */
  public boolean isEnvironmentBasedAuth() {
    return userId != null && !userId.isBlank();
  }

  /**
   * Returns true if this config uses config file authentication.
   *
   * @return true if configFilePath is set and userId is not set
   */
  public boolean isConfigFileAuth() {
    return !isEnvironmentBasedAuth() && configFilePath != null && !configFilePath.isBlank();
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
