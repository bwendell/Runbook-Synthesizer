package com.oracle.runbook.output.adapters;

import com.oracle.runbook.domain.DynamicChecklist;
import com.oracle.runbook.output.WebhookConfig;
import com.oracle.runbook.output.WebhookDestination;
import com.oracle.runbook.output.WebhookResult;
import java.util.concurrent.CompletableFuture;

/**
 * Slack webhook destination that formats checklists using Slack Block Kit.
 *
 * <p>This adapter integrates with Slack's incoming webhooks API to deliver formatted
 * troubleshooting checklists directly to Slack channels.
 *
 * <p><strong>Note:</strong> Full Block Kit formatting implementation is planned for v1.1.
 * Currently, this class throws {@link UnsupportedOperationException} when {@link #send} is called.
 *
 * <h2>Planned Features (v1.1)</h2>
 *
 * <ul>
 *   <li>Block Kit formatting with sections, dividers, and checkboxes
 *   <li>Rich text formatting for checklist items
 *   <li>Action buttons for marking items complete
 *   <li>Thread support for follow-up messages
 * </ul>
 *
 * @since 1.1 Full implementation available
 * @see WebhookDestination
 * @see <a href="https://api.slack.com/block-kit">Slack Block Kit</a>
 */
public class SlackWebhookDestination implements WebhookDestination {

  private final WebhookConfig config;

  /**
   * Creates a new SlackWebhookDestination with the given configuration.
   *
   * @param config the webhook configuration containing the Slack webhook URL
   */
  public SlackWebhookDestination(WebhookConfig config) {
    this.config = config;
  }

  @Override
  public String name() {
    return config.name();
  }

  @Override
  public String type() {
    return "slack";
  }

  @Override
  public WebhookConfig config() {
    return config;
  }

  @Override
  public CompletableFuture<WebhookResult> send(DynamicChecklist checklist) {
    return CompletableFuture.supplyAsync(
        () -> {
          throw new UnsupportedOperationException("Slack integration available in v1.1");
        });
  }

  @Override
  public boolean shouldSend(DynamicChecklist checklist) {
    // Default to true for now; filtering can be configured via WebhookFilter
    return true;
  }

  /**
   * Formats the given checklist as Slack Block Kit JSON.
   *
   * <p><strong>Note:</strong> This is a placeholder method for v1.1 implementation.
   *
   * @param checklist the dynamic checklist to format
   * @return null (placeholder for v1.1 implementation)
   * @since 1.1
   */
  public String formatAsBlockKit(DynamicChecklist checklist) {
    // Placeholder for v1.1 Block Kit implementation
    // Will return JSON structure like:
    // {
    // "blocks": [
    // { "type": "header", "text": { "type": "plain_text", "text": checklist.title()
    // } },
    // { "type": "section", "text": { "type": "mrkdwn", "text": item.description() }
    // },
    // ...
    // ]
    // }
    return null;
  }
}
