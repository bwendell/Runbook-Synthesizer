# Runbook-Synthesizer API Documentation

This document describes the REST API endpoints for Runbook-Synthesizer.

## Base URL

- **Development:** `http://localhost:8080`
- **Production:** `https://runbook-synthesizer.oraclecloud.com`

## Authentication

> **Note:** OCI IAM authentication will be added in a future phase. Currently, endpoints are unauthenticated.

---

## Endpoints

### Health Check

Check application health status for Kubernetes probes.

```bash
curl http://localhost:8080/api/v1/health
```

**Response:**

```json
{
  "status": "UP",
  "timestamp": "2026-01-16T00:00:00Z"
}
```

---

### Alert Ingestion

Ingest an alert and receive a generated troubleshooting checklist.

```bash
curl -X POST http://localhost:8080/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '{
    "title": "High CPU Usage",
    "message": "CPU utilization is above 95%",
    "severity": "CRITICAL",
    "sourceService": "oci-monitoring",
    "dimensions": {
      "compartmentId": "ocid1.compartment.oc1..xxx"
    },
    "labels": {
      "env": "production"
    }
  }'
```

**Response (200 OK):**

```json
{
  "alertId": "550e8400-e29b-41d4-a716-446655440000",
  "summary": "Generated checklist for: High CPU Usage",
  "steps": [
    {
      "order": 1,
      "instruction": "Check current status",
      "rationale": "First step in troubleshooting",
      "currentValue": "Unknown",
      "expectedValue": "Healthy",
      "priority": "HIGH",
      "commands": []
    }
  ],
  "sourceRunbooks": ["runbook-stub.md"],
  "generatedAt": "2026-01-16T00:00:00Z",
  "llmProviderUsed": "stub"
}
```

**Error Response (400 Bad Request):**

```json
{
  "correlationId": "uuid-string",
  "errorCode": "VALIDATION_ERROR",
  "message": "title is required",
  "timestamp": "2026-01-16T00:00:00Z",
  "details": {}
}
```

---

### List Webhooks

Get all configured webhook destinations.

```bash
curl http://localhost:8080/api/v1/webhooks
```

**Response (200 OK):**

```json
[
  {
    "name": "slack-production",
    "type": "SLACK",
    "url": "https://hooks.slack.com/xxx",
    "enabled": true,
    "filterSeverities": ["CRITICAL"],
    "headers": {}
  }
]
```

---

### Register Webhook

Add a new webhook destination for checklist notifications.

```bash
curl -X POST http://localhost:8080/api/v1/webhooks \
  -H "Content-Type: application/json" \
  -d '{
    "name": "slack-production",
    "type": "SLACK",
    "url": "https://hooks.slack.com/xxx",
    "enabled": true,
    "filterSeverities": ["CRITICAL", "WARNING"]
  }'
```

**Response (201 Created):**

```json
{
  "name": "slack-production",
  "type": "SLACK",
  "url": "https://hooks.slack.com/xxx",
  "enabled": true,
  "filterSeverities": ["CRITICAL", "WARNING"],
  "headers": {}
}
```

---

### Trigger Runbook Sync

Trigger re-indexing of runbooks from OCI Object Storage.

```bash
curl -X POST http://localhost:8080/api/v1/runbooks/sync \
  -H "Content-Type: application/json" \
  -d '{
    "bucketName": "my-runbooks",
    "prefix": "ops/",
    "forceRefresh": true
  }'
```

**Response (202 Accepted):**

```json
{
  "status": "STARTED",
  "documentsProcessed": 0,
  "errors": [],
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

## Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Request body validation failed |
| `NOT_FOUND` | 404 | Resource not found |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## OpenAPI Specification

The full OpenAPI 3.0 specification is available at:

- **File:** [`src/main/resources/META-INF/openapi.yaml`](../src/main/resources/META-INF/openapi.yaml)
