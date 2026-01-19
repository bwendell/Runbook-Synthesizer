package com.oracle.runbook.infrastructure.cloud.oci;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for creating OCI {@link AuthenticationDetailsProvider} instances.
 *
 * <p>This factory supports the following authentication methods in priority order:
 *
 * <ol>
 *   <li><strong>Resource Principals</strong> - When OCI_RESOURCE_PRINCIPAL_VERSION is set (OKE,
 *       Functions)
 *   <li><strong>Instance Principals</strong> - When running on OCI Compute (detected by metadata
 *       availability)
 *   <li><strong>Environment Variables</strong> - When OciConfig has userId/tenancyId/fingerprint
 *       set
 *   <li><strong>Config File</strong> - When OciConfig has configFilePath set
 * </ol>
 */
public final class OciAuthProviderFactory {

  private static final Logger LOGGER = Logger.getLogger(OciAuthProviderFactory.class.getName());

  /** Environment variable that indicates Resource Principals should be used. */
  public static final String RESOURCE_PRINCIPAL_VERSION_ENV = "OCI_RESOURCE_PRINCIPAL_VERSION";

  private OciAuthProviderFactory() {
    // Utility class
  }

  /**
   * Creates a {@link BasicAuthenticationDetailsProvider} based on the provided config.
   *
   * <p>Priority order: Resource Principals > Instance Principals > Environment Variables > Config
   * File
   *
   * @param config the OCI configuration (must not be null)
   * @return a BasicAuthenticationDetailsProvider for OCI SDK clients
   * @throws NullPointerException if config is null
   * @throws IllegalStateException if authentication cannot be established
   */
  public static BasicAuthenticationDetailsProvider create(OciConfig config) {
    Objects.requireNonNull(config, "OciConfig cannot be null");

    // Priority 1: Resource Principals (OKE, Functions - when OCI_RESOURCE_PRINCIPAL_VERSION is
    // set)
    if (isResourcePrincipalsEnabled()) {
      LOGGER.info("Using Resource Principals authentication (OCI_RESOURCE_PRINCIPAL_VERSION set)");
      return createResourcePrincipalsProvider();
    }

    // Priority 2: Instance Principals (OCI Compute - try if env-based and config file not
    // available)
    // Note: This is only attempted if explicitly requested or no other auth is configured
    if (shouldTryInstancePrincipals(config)) {
      try {
        LOGGER.info("Attempting Instance Principals authentication (running on OCI Compute)");
        return createInstancePrincipalsProvider();
      } catch (Exception e) {
        LOGGER.log(
            Level.FINE,
            "Instance Principals not available, falling back to other methods: " + e.getMessage());
      }
    }

    // Priority 3: Environment variable-based auth (when userId is set in config)
    if (config.isEnvironmentBasedAuth()) {
      LOGGER.fine("Using environment variable-based authentication");
      return createFromEnvironmentConfig(config);
    }

    // Priority 4: Config file authentication
    LOGGER.fine("Using config file authentication");
    return createFromConfigFile(config);
  }

  /**
   * Checks if Resource Principals authentication should be used.
   *
   * @return true if OCI_RESOURCE_PRINCIPAL_VERSION environment variable is set
   */
  public static boolean isResourcePrincipalsEnabled() {
    String rpVersion = System.getenv(RESOURCE_PRINCIPAL_VERSION_ENV);
    return rpVersion != null && !rpVersion.isBlank();
  }

  /**
   * Determines if Instance Principals should be attempted.
   *
   * <p>Instance Principals are tried when: - No environment-based auth is configured - No config
   * file is available - The application appears to be running in an OCI environment
   *
   * @param config the OCI configuration
   * @return true if Instance Principals should be attempted
   */
  private static boolean shouldTryInstancePrincipals(OciConfig config) {
    // Don't try if env-based auth is configured
    if (config.isEnvironmentBasedAuth()) {
      return false;
    }

    // Don't try if config file path is explicitly set and exists
    if (config.configFilePath() != null && !config.configFilePath().isBlank()) {
      String configPath = config.configFilePath();
      if (configPath.startsWith("~")) {
        configPath = System.getProperty("user.home") + configPath.substring(1);
      }
      if (Files.exists(Path.of(configPath))) {
        return false;
      }
    }

    // Check default config file location
    String defaultConfigPath = System.getProperty("user.home") + "/.oci/config";
    if (Files.exists(Path.of(defaultConfigPath))) {
      return false;
    }

    // No config file available - try Instance Principals
    return true;
  }

  /**
   * Creates a Resource Principals authentication provider.
   *
   * @return ResourcePrincipalAuthenticationDetailsProvider
   */
  private static BasicAuthenticationDetailsProvider createResourcePrincipalsProvider() {
    return ResourcePrincipalAuthenticationDetailsProvider.builder().build();
  }

  /**
   * Creates an Instance Principals authentication provider.
   *
   * @return InstancePrincipalsAuthenticationDetailsProvider
   */
  private static BasicAuthenticationDetailsProvider createInstancePrincipalsProvider() {
    return InstancePrincipalsAuthenticationDetailsProvider.builder().build();
  }

  /**
   * Creates an AuthenticationDetailsProvider from environment variable config values.
   *
   * @param config OciConfig with userId, tenancyId, fingerprint, and privateKey set
   * @return SimpleAuthenticationDetailsProvider
   */
  private static AuthenticationDetailsProvider createFromEnvironmentConfig(OciConfig config) {
    // Validate required fields
    if (config.userId() == null || config.userId().isBlank()) {
      throw new IllegalStateException("OCI_USER_ID is required for environment-based auth");
    }
    if (config.tenancyId() == null || config.tenancyId().isBlank()) {
      throw new IllegalStateException("OCI_TENANCY_ID is required for environment-based auth");
    }
    if (config.fingerprint() == null || config.fingerprint().isBlank()) {
      throw new IllegalStateException("OCI_FINGERPRINT is required for environment-based auth");
    }

    // Get private key supplier
    Supplier<InputStream> privateKeySupplier = getPrivateKeySupplier(config);

    // Parse region if provided
    Region region = null;
    if (config.region() != null && !config.region().isBlank()) {
      region = Region.fromRegionCodeOrId(config.region());
    }

    return SimpleAuthenticationDetailsProvider.builder()
        .userId(config.userId())
        .tenantId(config.tenancyId())
        .fingerprint(config.fingerprint())
        .privateKeySupplier(privateKeySupplier)
        .region(region)
        .build();
  }

  /**
   * Gets a private key supplier from either the key content or key file path in config.
   *
   * @param config OciConfig with privateKeyContent or privateKeyFilePath
   * @return Supplier that provides an InputStream of the private key
   */
  private static Supplier<InputStream> getPrivateKeySupplier(OciConfig config) {
    // Prefer inline content if available
    if (config.privateKeyContent() != null && !config.privateKeyContent().isBlank()) {
      return () ->
          new ByteArrayInputStream(config.privateKeyContent().getBytes(StandardCharsets.UTF_8));
    }

    // Fall back to file path
    if (config.privateKeyFilePath() != null && !config.privateKeyFilePath().isBlank()) {
      return () -> {
        try {
          String filePath = config.privateKeyFilePath();
          // Expand ~ to user home
          if (filePath.startsWith("~")) {
            filePath = System.getProperty("user.home") + filePath.substring(1);
          }
          return Files.newInputStream(Path.of(filePath));
        } catch (IOException e) {
          throw new IllegalStateException(
              "Failed to read private key file: " + config.privateKeyFilePath(), e);
        }
      };
    }

    throw new IllegalStateException(
        "Either OCI_PRIVATE_KEY_CONTENT or OCI_PRIVATE_KEY_FILE must be provided");
  }

  /**
   * Creates an AuthenticationDetailsProvider from a config file.
   *
   * @param config OciConfig with configFilePath and profile
   * @return ConfigFileAuthenticationDetailsProvider
   */
  private static AuthenticationDetailsProvider createFromConfigFile(OciConfig config) {
    String configFilePath = config.configFilePath();
    String profile = config.profile();

    // Use defaults if not specified
    if (configFilePath == null || configFilePath.isBlank()) {
      configFilePath = "~/.oci/config";
    }
    if (profile == null || profile.isBlank()) {
      profile = "DEFAULT";
    }

    // Expand ~ to user home
    if (configFilePath.startsWith("~")) {
      configFilePath = System.getProperty("user.home") + configFilePath.substring(1);
    }

    // Verify config file exists
    Path configPath = Path.of(configFilePath);
    if (!Files.exists(configPath)) {
      throw new IllegalStateException(
          "OCI config file not found: "
              + configFilePath
              + ". Please create an OCI config file or use environment variables.");
    }

    try {
      return new ConfigFileAuthenticationDetailsProvider(configFilePath, profile);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Failed to create OCI authentication provider from config file: " + e.getMessage(), e);
    }
  }
}
