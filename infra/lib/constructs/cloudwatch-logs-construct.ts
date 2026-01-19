import * as cdk from 'aws-cdk-lib';
import * as logs from 'aws-cdk-lib/aws-logs';
import { Construct } from 'constructs';

/**
 * CloudWatchLogsConstruct creates a log group for E2E test logging.
 * 
 * Features:
 * - Fixed log group name for consistent test configuration
 * - 1-day retention to minimize costs
 * - Idempotent deployment
 */
export class CloudWatchLogsConstruct extends Construct {
  public readonly logGroup: logs.LogGroup;

  constructor(scope: Construct, id: string) {
    super(scope, id);

    this.logGroup = new logs.LogGroup(this, 'E2eLogGroup', {
      logGroupName: '/runbook-synthesizer/e2e',
      retention: logs.RetentionDays.ONE_DAY, // Minimize costs
      removalPolicy: cdk.RemovalPolicy.DESTROY, // Allow cleanup
    });

    // Apply standard tags
    cdk.Tags.of(this.logGroup).add('ManagedBy', 'runbook-synthesizer-e2e');
    cdk.Tags.of(this.logGroup).add('Environment', 'test');
  }
}
