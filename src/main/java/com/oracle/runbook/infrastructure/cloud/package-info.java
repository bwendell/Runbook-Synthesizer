/**
 * Cloud provider abstraction layer for multi-cloud support.
 *
 * <p>This package contains the core interfaces and abstractions that enable the application to work
 * with multiple cloud providers (AWS, OCI, etc.) through a unified API. The design follows
 * Hexagonal Architecture principles with:
 *
 * <ul>
 *   <li>{@code CloudConfig} - Base configuration interface for all cloud providers
 *   <li>{@code CloudStorageAdapter} - Abstract storage operations (S3, Object Storage)
 *   <li>{@code ComputeMetadataAdapter} - Abstract compute instance metadata retrieval
 * </ul>
 *
 * <p>Provider-specific implementations are located in subpackages:
 *
 * <ul>
 *   <li>{@code oci} - Oracle Cloud Infrastructure implementations
 *   <li>{@code aws} - Amazon Web Services implementations
 * </ul>
 *
 * @see com.oracle.runbook.infrastructure.cloud.oci
 * @see com.oracle.runbook.infrastructure.cloud.aws
 */
package com.oracle.runbook.infrastructure.cloud;
