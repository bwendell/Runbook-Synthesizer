package com.oracle.runbook.config;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Factory for creating OCI {@link AuthenticationDetailsProvider} instances.
 * <p>
 * This factory supports the following authentication methods in order of
 * priority:
 * <ol>
 * <li>Config file authentication (when config file exists)</li>
 * <li>Instance principals (when running on OCI - future enhancement)</li>
 * <li>Environment variables (future enhancement)</li>
 * </ol>
 */
public final class OciAuthProviderFactory {

	private OciAuthProviderFactory() {
		// Utility class
	}

	/**
	 * Creates an {@link AuthenticationDetailsProvider} based on the provided
	 * config.
	 * <p>
	 * Currently supports config file authentication. Future versions will add
	 * instance principal and environment variable support.
	 *
	 * @param config
	 *            the OCI configuration (must not be null)
	 * @return an AuthenticationDetailsProvider for OCI SDK clients
	 * @throws NullPointerException
	 *             if config is null
	 * @throws IllegalStateException
	 *             if authentication cannot be established
	 */
	public static AuthenticationDetailsProvider create(OciConfig config) {
		Objects.requireNonNull(config, "OciConfig cannot be null");

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
			throw new IllegalStateException("OCI config file not found: " + configFilePath
					+ ". Please create an OCI config file or use instance principals.");
		}

		try {
			return new ConfigFileAuthenticationDetailsProvider(configFilePath, profile);
		} catch (IOException e) {
			throw new IllegalStateException(
					"Failed to create OCI authentication provider from config file: " + e.getMessage(), e);
		}
	}
}
