/**
 * Application configuration and initialization.
 *
 * <p>Configuration for:
 *
 * <ul>
 *   <li>Server settings (port, host)
 *   <li>LLM provider configuration
 *   <li>OCI credentials
 *   <li>Webhook destinations
 * </ul>
 *
 * <h2>OCI Configuration</h2>
 *
 * <ul>
 *   <li>{@link com.oracle.runbook.config.OciConfig} - OCI authentication settings (compartmentId,
 *       region, profile)
 *   <li>{@link com.oracle.runbook.config.OciAuthProviderFactory} - Creates OCI SDK authentication
 *       providers
 * </ul>
 */
package com.oracle.runbook.config;
