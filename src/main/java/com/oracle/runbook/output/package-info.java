/**
 * Webhook destinations for generated checklists.
 * <p>
 * Supports multiple output channels:
 * <ul>
 *   <li>Slack - Block Kit formatting</li>
 *   <li>PagerDuty - Events API v2</li>
 *   <li>Generic - Custom HTTP webhooks</li>
 * </ul>
 * 
 * <h2>Ports/Interfaces (Hexagonal Architecture)</h2>
 * <ul>
 *   <li>{@link com.oracle.runbook.output.WebhookDestination} - Interface for webhook output channels</li>
 *   <li>{@link com.oracle.runbook.output.WebhookResult} - Result record for webhook responses</li>
 * </ul>
 */
package com.oracle.runbook.output;

