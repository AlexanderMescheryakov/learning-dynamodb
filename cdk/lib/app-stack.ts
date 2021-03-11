import { App, CfnOutput, Duration, RemovalPolicy, Stack, StackProps } from '@aws-cdk/core';
import { PolicyStatement, Role, ServicePrincipal } from '@aws-cdk/aws-iam';
import { IEventSource, IFunction, StartingPosition, Tracing } from '@aws-cdk/aws-lambda';
import { DynamoEventSource } from '@aws-cdk/aws-lambda-event-sources';
import * as lambda from '@aws-cdk/aws-lambda';
import * as apigateway from '@aws-cdk/aws-apigateway';
import { DynamoDbTable } from './dynamo';
import { BillingMode, StreamViewType } from '@aws-cdk/aws-dynamodb';
import { IUserPool, UserPool } from '@aws-cdk/aws-cognito';

export interface LambdaProps {
  handlerName: string;
  environment?: Record<string, string>;
  policyStatements?: PolicyStatement[];
  timeout?: Duration;
  memorySize?: number;
  assetsPath?: string;
  nativeJavaRuntime?: boolean;
  events?: IEventSource[];
  tracing?: boolean;
}

export interface AppStackProps extends StackProps {
  deploymentEnv: string;
  javaLambdaPath: string;
  nativeJavaRuntime: boolean;
  tracing?: boolean;
}

export interface ApiResources {
  lambda: IFunction;
  apiResource: apigateway.Resource;
}

const DEFAULT_LAMBDA_TIMEOUT = Duration.minutes(1);
const LAMBDA_JVM_MEMORY = 384;
const LAMBDA_NATIVE_MEMORY = 128;
const QUARKUS_HANDLER = 'io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest';
const DYNAMODB_STREAM_HANDLER_BATCH = 25;
const RCU_MIN = 5;
const RCU_MAX = 40000;
const WCU_MIN = 5;
const WCU_MAX = 40000;
const UTILIZATION_TARGET = 80;

export class AppStack extends Stack {
  private readonly props: AppStackProps;
  private readonly lambdaPolicies: PolicyStatement;
  private readonly dynamoDbTable: DynamoDbTable;
  private readonly dynamoDbPaymentsTable: DynamoDbTable;
  private readonly restApi: apigateway.RestApi;
  private readonly userPool: IUserPool;
  private readonly authorizer: apigateway.CfnAuthorizer;

  public constructor(app: App, id: string, props: AppStackProps) {
    super(app, id, props);
    this.props = props;
    this.lambdaPolicies = this.buildPolicyStatement(['logs:*'], ['arn:aws:logs:*:*:*']);
    this.dynamoDbTable = this.setupDynamoDbTable();
    this.dynamoDbPaymentsTable = this.setupDynamoDbPaymentsTable();
    this.restApi = this.setupApiGateway();
    this.userPool = this.initUserPool();
    this.initUserPoolClient();
    this.authorizer = this.initAuthorizer();
  }

  public defineRestApi(method: string, url: string, handlerName: string, restrict = false): ApiResources {
    const lambda = this.defineJavaQuarkusLambda('market-api', {
      handlerName,
      environment: {
        DYNAMODB_TABLE: this.dynamoDbTable.getTable().tableName,
        DYNAMODB_TABLE_PAYMENTS: this.dynamoDbPaymentsTable.getTable().tableName,
      },
      policyStatements: [this.lambdaPolicies],
      nativeJavaRuntime: this.props.nativeJavaRuntime,
      tracing: this.props.tracing,
    });

    this.dynamoDbTable.getTable().grantReadWriteData(lambda);
    this.dynamoDbPaymentsTable.getTable().grantReadWriteData(lambda);
    const apiResource = this.restApi.root.resourceForPath(url);
    const lambdaIntegration = new apigateway.LambdaIntegration(lambda);
    const options = restrict
      ? {
          authorizationType: apigateway.AuthorizationType.COGNITO,
          authorizer: {
            authorizerId: this.authorizer.ref,
          },
        }
      : undefined;
    apiResource.addMethod(method, lambdaIntegration, options);
    return { lambda, apiResource };
  }

  public defineStreamLambda(handlerName: string): lambda.Function {
    const lambda = this.defineJavaQuarkusLambda('market-api', {
      handlerName,
      environment: {
        DYNAMODB_TABLE: this.dynamoDbTable.getTable().tableName,
      },
      policyStatements: [this.lambdaPolicies],
      nativeJavaRuntime: this.props.nativeJavaRuntime,
      events: [
        new DynamoEventSource(this.dynamoDbTable.getTable(), {
          startingPosition: StartingPosition.LATEST,
          batchSize: DYNAMODB_STREAM_HANDLER_BATCH,
          retryAttempts: 1,
        }),
      ],
    });

    this.dynamoDbTable.getTable().grantReadWriteData(lambda);
    this.dynamoDbTable.getTable().grantStreamRead(lambda);
    return lambda;
  }

  private setupDynamoDbTable(): DynamoDbTable {
    const tableName = 'LearningDynamoDb';
    const table = new DynamoDbTable(this, tableName, {
      tableName,
      globalIndexes: [{ projected: ['PK', 'Data'] }, { projected: ['PK'] }],
      deploymentEnv: this.props.deploymentEnv,
      removalPolicy: RemovalPolicy.DESTROY,
      streamType: StreamViewType.NEW_AND_OLD_IMAGES,
    });
    table.setAutoScaling(
      { minCapacity: RCU_MIN, maxCapacity: RCU_MAX },
      { minCapacity: WCU_MIN, maxCapacity: WCU_MAX },
      UTILIZATION_TARGET,
    );
    table.setGsiAutoScaling(
      'GSI1',
      { minCapacity: RCU_MIN, maxCapacity: RCU_MAX },
      { minCapacity: WCU_MIN, maxCapacity: WCU_MAX },
      UTILIZATION_TARGET,
    );
    table.setGsiAutoScaling(
      'GSI2',
      { minCapacity: RCU_MIN, maxCapacity: RCU_MAX },
      { minCapacity: WCU_MIN, maxCapacity: WCU_MAX },
      UTILIZATION_TARGET,
    );
    return table;
  }

  private setupDynamoDbPaymentsTable(): DynamoDbTable {
    const tableName = 'LearningDynamoDb-Payments';
    return new DynamoDbTable(this, tableName, {
      tableName,
      deploymentEnv: this.props.deploymentEnv,
      removalPolicy: RemovalPolicy.DESTROY,
      streamType: StreamViewType.NEW_AND_OLD_IMAGES,
      billingMode: BillingMode.PAY_PER_REQUEST,
    });
  }

  private setupApiGateway(): apigateway.RestApi {
    const api = new apigateway.RestApi(this, 'learning-dynamodb', {
      defaultCorsPreflightOptions: {
        allowOrigins: apigateway.Cors.ALL_ORIGINS,
        allowMethods: apigateway.Cors.ALL_METHODS,
      },
    });
    api.root.addMethod('ANY');
    return api;
  }

  private initUserPool(): IUserPool {
    const userPool = new UserPool(this, 'UserPool', {
      signInAliases: {
        email: true,
      },
      selfSignUpEnabled: false,
    });
    new CfnOutput(this, 'UserPoolId', { value: userPool.userPoolId });
    return userPool;
  }

  private initUserPoolClient() {
    const client = this.userPool.addClient('API', {
      authFlows: {
        userPassword: true,
        userSrp: true,
      },
    });
    new CfnOutput(this, 'UserPoolAppClientId', { value: client.userPoolClientId });
    return client;
  }

  private initAuthorizer(): apigateway.CfnAuthorizer {
    return new apigateway.CfnAuthorizer(this, `market-api-authorizer-${this.props.deploymentEnv}`, {
      restApiId: this.restApi.restApiId,
      name: `market-api-${this.props.deploymentEnv}`,
      type: 'COGNITO_USER_POOLS',
      identitySource: 'method.request.header.Authorization',
      providerArns: [this.userPool.userPoolArn],
    });
  }

  private getLambdaRole(lambdaName: string): Role {
    return new Role(this, `${lambdaName}-role`, {
      roleName: lambdaName,
      assumedBy: new ServicePrincipal('lambda.amazonaws.com'),
    });
  }

  private defineJavaQuarkusLambda(lambdaGroup: string, props: LambdaProps): lambda.Function {
    const defaultJavaEnv = this.getQuarkusJvmEnv(props);
    const lambdaName = `${lambdaGroup}-${props.handlerName}`;
    const role = this.getLambdaRole(lambdaName);
    const environment = Object.assign(defaultJavaEnv, this.getCommonEnvVars(props, role.roleArn));
    const funcProps: lambda.FunctionProps = {
      environment,
      role,
      timeout: props.timeout || DEFAULT_LAMBDA_TIMEOUT,
      handler: QUARKUS_HANDLER,
      functionName: `${lambdaName}-${this.props.deploymentEnv}`,
      code: new lambda.AssetCode(`${this.props.javaLambdaPath}/${lambdaGroup}/build/function.zip`),
      runtime: props.nativeJavaRuntime ? lambda.Runtime.PROVIDED_AL2 : lambda.Runtime.JAVA_11,
      memorySize: props.memorySize || this.getQuarkusMemorySize(props),
      events: props.events,
      tracing: props.tracing ? Tracing.ACTIVE : Tracing.DISABLED,
    };

    const func = new lambda.Function(this, lambdaName, funcProps);
    func.addToRolePolicy(this.buildPolicyStatement(['sts:AssumeRole'], [role.roleArn]));
    return this.configureLambda(func, props);
  }

  private getQuarkusMemorySize(props: LambdaProps): number {
    return props.nativeJavaRuntime ? LAMBDA_NATIVE_MEMORY : LAMBDA_JVM_MEMORY;
  }

  private getQuarkusJvmEnv(props: LambdaProps): Record<string, string> {
    if (props.nativeJavaRuntime) {
      return {
        QUARKUS_LAMBDA_HANDLER: props.handlerName,
        DISABLE_SIGNAL_HANDLERS: 'true',
      };
    } else {
      return {
        JAVA_TOOL_OPTIONS: `-Dquarkus.lambda.handler=${props.handlerName}`,
      };
    }
  }

  private configureLambda(func: lambda.Function, props: LambdaProps) {
    if (props.policyStatements) {
      for (const statement of props.policyStatements) {
        func.addToRolePolicy(statement);
      }
    }

    return func;
  }

  private getCommonEnvVars(props: LambdaProps, executionRoleArn: string): Record<string, string> {
    const envVars = Object.assign({}, props.environment);
    envVars.ENV = this.props.deploymentEnv;
    envVars.ACCOUNT = this.account;
    envVars.ROLE_ARN = executionRoleArn;
    return envVars;
  }

  private buildPolicyStatement(actions: string[], resources: string[]): PolicyStatement {
    return new PolicyStatement({
      actions,
      resources,
    });
  }
}
