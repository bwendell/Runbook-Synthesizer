package com.oracle.runbook.config;

import java.util.Objects;

/**
 * Configuration record for OCI authentication and connectivity settings.
 * <p>
 * Holds the necessary configuration for connecting to OCI services,
 * including the compartment ID, region, config file path, and profile name.
 *
 * @param compartmentId the OCI compartment OCID (required)
 * @param region        the OCI region identifier (optional, e.g., "us-ashburn-1")
 * @param configFilePath the path to OCI config file (optional, defaults to ~/.oci/config)
 * @param profile       the OCI config profile name (optional, defaults to "DEFAULT")
 */
public record OciConfig(
    String compartmentId,
    String region,
    String configFilePath,
    String profile
) {

    /**
     * Compact constructor with validation.
     */
    public OciConfig {
        Objects.requireNonNull(compartmentId, "OciConfig compartmentId cannot be null");
    }

    /**
     * Factory method that creates an OciConfig with sensible defaults.
     * <p>
     * Uses ~/.oci/config as the config file path and "DEFAULT" as the profile.
     *
     * @param compartmentId the OCI compartment OCID (required)
     * @return a new OciConfig instance with default values for optional fields
     */
    public static OciConfig withDefaults(String compartmentId) {
        return new OciConfig(
            compartmentId,
            null,
            "~/.oci/config",
            "DEFAULT"
        );
    }
}
