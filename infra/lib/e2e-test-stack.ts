import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { S3Construct } from './constructs/s3-construct';
import { CloudWatchLogsConstruct } from './constructs/cloudwatch-logs-construct';
import { CloudWatchMetricsConstruct } from './constructs/cloudwatch-metrics-construct';

export class E2eTestStack extends cdk.Stack {
  public readonly s3Construct: S3Construct;
  public readonly cloudWatchLogsConstruct: CloudWatchLogsConstruct;
  public readonly cloudWatchMetricsConstruct: CloudWatchMetricsConstruct;

  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // S3 bucket for runbook storage
    this.s3Construct = new S3Construct(this, 'S3');

    // CloudWatch Logs for application logs
    this.cloudWatchLogsConstruct = new CloudWatchLogsConstruct(this, 'CloudWatchLogs');

    // CloudWatch Metrics (placeholder for future alarms/dashboards)
    this.cloudWatchMetricsConstruct = new CloudWatchMetricsConstruct(this, 'CloudWatchMetrics');

    // Stack outputs
    new cdk.CfnOutput(this, 'BucketName', {
      value: this.s3Construct.bucket.bucketName,
      description: 'S3 bucket name for runbook storage',
      exportName: 'RunbookSynthesizerE2eBucketName',
    });

    new cdk.CfnOutput(this, 'LogGroupName', {
      value: this.cloudWatchLogsConstruct.logGroup.logGroupName,
      description: 'CloudWatch log group name',
      exportName: 'RunbookSynthesizerE2eLogGroupName',
    });

    new cdk.CfnOutput(this, 'LogGroupArn', {
      value: this.cloudWatchLogsConstruct.logGroup.logGroupArn,
      description: 'CloudWatch log group ARN',
      exportName: 'RunbookSynthesizerE2eLogGroupArn',
    });
  }
}
