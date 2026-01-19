#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { E2eTestStack } from '../lib/e2e-test-stack';

const app = new cdk.App();

new E2eTestStack(app, 'RunbookSynthesizerE2eStack', {
  description: 'AWS resources for Runbook Synthesizer E2E testing',
  env: {
    // Use account/region from environment or default
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: process.env.CDK_DEFAULT_REGION ?? 'us-west-2',
  },
  tags: {
    ManagedBy: 'runbook-synthesizer-e2e',
    Environment: 'test',
  },
});
