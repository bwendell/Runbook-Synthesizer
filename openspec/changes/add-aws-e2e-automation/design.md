## Context

The Runbook-Synthesizer project supports both OCI and AWS cloud providers. Integration tests exist for LocalStack-based AWS testing, but there's no automation for testing against real AWS services. This change introduces:

1. **AWS CDK (TypeScript)** for infrastructure-as-code provisioning
2. **Extensible construct pattern** for easy addition of future AWS services
3. **Java e2e tests** that validate behavior against real AWS

## Goals / Non-Goals

**Goals:**
- Automated, idempotent AWS resource provisioning via CDK
- Manual cleanup (no automatic teardown) for cost control
- Extensible architecture for adding new AWS services
- Tests disabled by default (opt-in via environment variable)

**Non-Goals:**
- CI/CD pipeline integration (future work)
- Multi-region support (single region for now)
- Auto-scaling or production-grade infrastructure

## Decisions

### Decision 1: AWS CDK (TypeScript) over Java CDK
- **Rationale**: TypeScript is CDK's reference implementation with best community support, faster feature updates, and more examples available
- **Alternative considered**: Java CDK - rejected because it has more boilerplate and fewer community resources

### Decision 2: Separate `infra/` directory
- **Rationale**: Clean separation between application code (Java) and infrastructure code (TypeScript)
- **Alternative considered**: Embedding CDK in Maven build - rejected due to complexity mixing npm and Maven lifecycles

### Decision 3: Extensible Construct Pattern
- **Rationale**: Each AWS service is a separate construct (`S3Construct`, `CloudWatchLogsConstruct`) enabling easy addition of new services (Lambda, DynamoDB) in future
- **Trade-off**: Slightly more files, but better maintainability

### Decision 4: Manual Cleanup Only
- **Rationale**: User explicitly requested manual cleanup for cost control. Resources are idempotent and reusable between test runs
- **Command**: `npm run cdk:destroy` when cleanup needed

## Architecture

```
infra/
├── package.json          # CDK dependencies + scripts
├── cdk.json              # CDK configuration  
├── tsconfig.json         # TypeScript config
├── bin/
│   └── e2e-infra.ts      # CDK app entry point
└── lib/
    ├── e2e-test-stack.ts # Main stack
    └── constructs/
        ├── s3-construct.ts
        ├── cloudwatch-logs-construct.ts
        └── cloudwatch-metrics-construct.ts
```

## Resource Naming

| Resource | Name Pattern | Notes |
|----------|--------------|-------|
| S3 Bucket | `runbook-synthesizer-e2e-{accountId}` | Globally unique |
| Log Group | `/runbook-synthesizer/e2e` | Fixed name |
| Stack | `RunbookSynthesizerE2eStack` | CloudFormation stack |

## Tags

All resources tagged with:
- `ManagedBy=runbook-synthesizer-e2e`
- `Environment=test`

## Open Questions

None - design approved by user.
