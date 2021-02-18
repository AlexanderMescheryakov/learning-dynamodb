import { App } from '@aws-cdk/core';

import { AppStack } from './app-stack';

const app = new App();

const deploymentEnv = app.node.tryGetContext('env') || 'dev';
const account = app.node.tryGetContext('account');
const region = app.node.tryGetContext('region') || 'us-east-1';

new AppStack(app, `learning-dynamodb-${deploymentEnv}`, {
  deploymentEnv,
  env: {
    account,
    region,
  },
});

app.synth();
