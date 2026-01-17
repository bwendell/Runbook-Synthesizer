package com.oracle.runbook.api.dto;

/**
 * Request body for POST /api/v1/runbooks/sync endpoint.
 *
 * @param bucketName optional bucket name to sync from (null = all configured)
 * @param prefix optional prefix to filter runbooks within bucket
 * @param forceRefresh if true, re-index even if documents haven't changed
 */
public record SyncRequest(String bucketName, String prefix, boolean forceRefresh) {}
