import { App } from '@aws-cdk/core';

import { AppStack } from './app-stack';

const app = new App();

const deploymentEnv = app.node.tryGetContext('env') || 'dev';
const account = app.node.tryGetContext('account');
const region = app.node.tryGetContext('region') || 'us-east-1';
const nativeJavaRuntime = app.node.tryGetContext('jvmRuntime') == 'native';

const stack = new AppStack(app, `learning-dynamodb-${deploymentEnv}`, {
  deploymentEnv,
  nativeJavaRuntime,
  env: {
    account,
    region,
  },
  javaLambdaPath: '../java-lambda/',
});

stack.defineStreamLambda('dynamodb-stream-handler');

stack.defineRestApi('GET', '/order', 'get-order');
stack.defineRestApi('GET', '/orders', 'get-orders');
stack.defineRestApi('POST', '/order', 'add-order');
stack.defineRestApi('PUT', '/order', 'update-order');
stack.defineRestApi('DELETE', '/order', 'delete-order');

stack.defineRestApi('GET', '/customer', 'get-customer');
stack.defineRestApi('POST', '/customer', 'add-customer');
stack.defineRestApi('PUT', '/customer', 'update-customer');

stack.defineRestApi('GET', '/product', 'get-product');
stack.defineRestApi('GET', '/products', 'get-products');
stack.defineRestApi('POST', '/product', 'add-product');
stack.defineRestApi('PUT', '/product', 'update-product');
stack.defineRestApi('DELETE', '/product', 'delete-product');

stack.defineRestApi('POST', '/payment', 'add-payment');
stack.defineRestApi('GET', '/payments', 'get-payments', true);

app.synth();
