# Change: Add Real AWS E2E Test Automation

## Why

The project currently lacks automated provisioning for real AWS e2e tests. Manual steps are required per [AWS_FREE_TIER_TESTING_GUIDE.md](file:///c:/Users/bwend/repos/ops-scribe/docs/AWS_FREE_TIER_TESTING_GUIDE.md), which limits CI/CD integration and makes testing inconsistent.

## What Changes

- Add AWS CDK (TypeScript) project under `infra/` for infrastructure-as-code provisioning
- Create extensible CDK constructs for S3, CloudWatch Logs, and CloudWatch Metrics
- Add Java e2e test classes that run against real AWS services
- Add Maven profile `e2e-aws-real` for isolated real AWS test execution
- Update documentation with CDK-based automation workflow

## Impact

- **Affected specs**: `integration-tests` (new requirements for real AWS e2e)
- **Affected code**: New `infra/` directory, new `src/test/java/.../aws/e2e/` test package
- **Breaking changes**: None - additive feature only
