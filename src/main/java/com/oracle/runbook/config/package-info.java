/**
 * Application configuration and initialization.
 * <p>
 * Configuration for:
 * <ul>
 * <li>Server settings (port, host)</li>
 * <li>LLM provider configuration</li>
 * <li>OCI credentials</li>
 * <li>Webhook destinations</li>
 * </ul>
 * 
 * <h2>OCI Configuration</h2>
 * <ul>
 * <li>{@link com.oracle.runbook.config.OciConfig} - OCI authentication settings
 * (compartmentId, region, profile)</li>
 * <li>{@link com.oracle.runbook.config.OciAuthProviderFactory} - Creates OCI
 * SDK authentication providers</li>
 * </ul>
 */
package com.oracle.runbook.config;
