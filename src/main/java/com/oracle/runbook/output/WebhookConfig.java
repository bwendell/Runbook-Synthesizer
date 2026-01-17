package com.oracle.runbook.output;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Configuration record for a webhook destination.
 *
 * <p>Contains all settings needed to create and configure a webhook destination including URL,
 * authentication headers, filtering rules, and enabled status.
 *
 * @param name unique identifier for this webhook destination
 * @param type the type of destination (e.g., "slack", "pagerduty", "generic")
 * @param url the webhook endpoint URL
 * @param enabled whether this webhook is active
 * @param filter filtering rules for which checklists to send
 * @param headers HTTP headers to include in requests
 */
public record WebhookConfig(
    String name,
    String type,
    String url,
    boolean enabled,
    WebhookFilter filter,
    Map<String, String> headers) {

  /** Compact constructor with defensive copy for headers. */
  public WebhookConfig {
    headers = headers != null ? Map.copyOf(headers) : Map.of();
  }

  /**
   * Creates a new builder for WebhookConfig.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for ergonomic WebhookConfig construction with validation. */
  public static final class Builder {
    private String name;
    private String type;
    private String url;
    private boolean enabled = true;
    private WebhookFilter filter;
    private Map<String, String> headers = Map.of();

    private Builder() {}

    /**
     * Sets the webhook name.
     *
     * @param name the unique identifier
     * @return this builder
     */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the webhook type.
     *
     * @param type the destination type
     * @return this builder
     */
    public Builder type(String type) {
      this.type = type;
      return this;
    }

    /**
     * Sets the webhook URL.
     *
     * @param url the endpoint URL
     * @return this builder
     */
    public Builder url(String url) {
      this.url = url;
      return this;
    }

    /**
     * Sets whether the webhook is enabled.
     *
     * @param enabled true to enable
     * @return this builder
     */
    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Sets the filter for this webhook.
     *
     * @param filter the filter configuration
     * @return this builder
     */
    public Builder filter(WebhookFilter filter) {
      this.filter = filter;
      return this;
    }

    /**
     * Sets the HTTP headers for this webhook.
     *
     * @param headers the headers map
     * @return this builder
     */
    public Builder headers(Map<String, String> headers) {
      this.headers = headers;
      return this;
    }

    /**
     * Builds the WebhookConfig with validation.
     *
     * @return the constructed WebhookConfig
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    public WebhookConfig build() {
      if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("WebhookConfig name is required");
      }
      if (url == null || url.isBlank()) {
        throw new IllegalArgumentException("WebhookConfig url is required");
      }
      if (type == null || type.isBlank()) {
        throw new IllegalArgumentException("WebhookConfig type must be non-empty");
      }

      // Validate URL format
      try {
        new URI(url);
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
          throw new IllegalArgumentException("WebhookConfig url must be a valid HTTP/HTTPS URL");
        }
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException(
            "WebhookConfig url must be a valid URL: " + e.getMessage());
      }

      // Default filter to allowAll if not set
      WebhookFilter effectiveFilter = filter != null ? filter : WebhookFilter.allowAll();

      return new WebhookConfig(name, type, url, enabled, effectiveFilter, headers);
    }
  }
}
