package com.oracle.runbook.domain;

/**
 * Priority levels for checklist steps. Ordered by urgency: HIGH (most urgent) to LOW (least
 * urgent).
 */
public enum StepPriority {
  /** High priority - should be addressed first. */
  HIGH,

  /** Medium priority - important but not critical. */
  MEDIUM,

  /** Low priority - can be addressed after higher priority items. */
  LOW
}
