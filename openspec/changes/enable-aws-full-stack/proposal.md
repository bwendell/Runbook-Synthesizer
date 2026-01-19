# Proposal: Enable AWS Full-Stack Functionality

## Summary

Enable complete AWS-first functionality for Runbook-Synthesizer, providing production-ready support for:
- Alert ingestion via CloudWatch Alarms (SNS webhook)
- LLM operations via AWS Bedrock (Claude 3 Haiku + Cohere Embed v3)
- Factory wiring for all cloud adapters
- AWS as default cloud provider

## Motivation

AWS is now the primary target platform. The application has existing AWS implementations for storage, compute metadata, metrics, and logs, but lacks:
1. Alert ingestion adapter for CloudWatch Alarms
2. LLM provider for AWS Bedrock
3. Complete factory wiring for all adapter types
4. Configuration defaults for AWS

## Scope

### In Scope
- `AwsSnsAlertSourceAdapter` for CloudWatch Alarms via SNS
- `AwsBedrockLlmProvider` for text generation and embeddings
- `CloudAdapterFactory` enhancement with metrics/logs/alerts/LLM factory methods
- `application.yaml` updates (AWS default, complete AWS config)
- AWS SDK dependencies (`bedrockruntime`, `sns`)
- E2E test integration with existing CDK infrastructure

### Out of Scope
- OCI enhancements (deferred unless simple fixes)
- New vector store implementations
- New webhook destinations

## Success Criteria

- [ ] CloudWatch Alarm JSON parsed correctly via `AwsSnsAlertSourceAdapter`
- [ ] Bedrock embeddings generated with Cohere Embed v3
- [ ] Bedrock text generation working with Claude 3 Haiku
- [ ] All adapters retrievable via `CloudAdapterFactory`
- [ ] Application starts with `cloud.provider: aws` by default
- [ ] All new components have unit tests
- [ ] E2E tests pass against real AWS resources
