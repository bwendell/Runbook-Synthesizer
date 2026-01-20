package com.oracle.runbook.output.adapters;

import java.util.Objects;

/**
 * Configuration for the file-based output adapter.
 *
 * @param outputDirectory the directory where checklist files will be written
 * @param name the name identifier for this output destination
 */
public record FileOutputConfig(String outputDirectory, String name) {
  /** Compact constructor with validation. */
  public FileOutputConfig {
    Objects.requireNonNull(outputDirectory, "FileOutputConfig outputDirectory cannot be null");
    Objects.requireNonNull(name, "FileOutputConfig name cannot be null");
  }
}
