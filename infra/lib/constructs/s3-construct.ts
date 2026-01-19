import * as cdk from 'aws-cdk-lib';
import * as s3 from 'aws-cdk-lib/aws-s3';
import { Construct } from 'constructs';

/**
 * S3Construct creates an S3 bucket for runbook storage in E2E tests.
 * 
 * Features:
 * - Globally unique bucket name using account ID
 * - Encryption at rest
 * - Versioning disabled (test data)
 * - Idempotent deployment
 */
export class S3Construct extends Construct {
  public readonly bucket: s3.Bucket;

  constructor(scope: Construct, id: string) {
    super(scope, id);

    // Get account ID for globally unique bucket name
    const accountId = cdk.Stack.of(this).account;

    this.bucket = new s3.Bucket(this, 'RunbookBucket', {
      bucketName: `runbook-synthesizer-e2e-${accountId}`,
      removalPolicy: cdk.RemovalPolicy.DESTROY, // Allow cleanup
      autoDeleteObjects: true, // Clean up on destroy
      encryption: s3.BucketEncryption.S3_MANAGED,
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      versioned: false, // Not needed for test data
    });

    // Apply standard tags
    cdk.Tags.of(this.bucket).add('ManagedBy', 'runbook-synthesizer-e2e');
    cdk.Tags.of(this.bucket).add('Environment', 'test');
  }
}
