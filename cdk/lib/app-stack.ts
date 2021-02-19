import { App, Duration, RemovalPolicy, Stack, StackProps } from '@aws-cdk/core';
import { AttributeType, ITable, ProjectionType, Table } from '@aws-cdk/aws-dynamodb';
import { IRole, PolicyStatement } from '@aws-cdk/aws-iam';
import { IFunction } from '@aws-cdk/aws-lambda';
import * as lambda from '@aws-cdk/aws-lambda';
import * as apigateway from '@aws-cdk/aws-apigateway';

export interface LambdaProps {
  handlerName: string;
  environment?: Record<string, string>;
  policyStatements?: PolicyStatement[];
  lambdaRole?: IRole;
  timeout?: Duration;
  memorySize?: number;
  assetsPath?: string;
}

export interface AppStackProps extends StackProps {
  deploymentEnv: string;
  javaLambdaPath: string;
}

export interface ApiResources {
  lambda: IFunction;
  apiResource: apigateway.Resource;
}

const DEFAULT_LAMBDA_TIMEOUT = Duration.minutes(1);
const LAMBDA_JAVA_MEMORY_LIMIT = 256;
const QUARKUS_HANDLER = 'io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest';

export class AppStack extends Stack {
  private readonly props: AppStackProps;
  private readonly lambdaPolicies: PolicyStatement;
  private readonly dynamoDbTable: ITable;
  private readonly restApi: apigateway.RestApi;

  public constructor(app: App, id: string, props: AppStackProps) {
    super(app, id, props);
    this.props = props;
    this.lambdaPolicies = this.buildPolicyStatement(['logs:*'], ['arn:aws:logs:*:*:*']);
    this.dynamoDbTable = this.setupDynamoDbTable();
    this.restApi = this.setupApiGateway();
  }

  public defineRestApi(method: string, url: string, handlerName: string): ApiResources {
    const lambda = this.defineJavaQuarkusLambda('market-api', {
      handlerName,
      environment: {
        DYNAMODB_TABLE: this.dynamoDbTable.tableName,
      },
      policyStatements: [this.lambdaPolicies],
      memorySize: LAMBDA_JAVA_MEMORY_LIMIT,
    });

    this.dynamoDbTable.grantReadWriteData(lambda);
    const apiResource = this.restApi.root.resourceForPath(url);
    const lambdaIntegration = new apigateway.LambdaIntegration(lambda);
    apiResource.addMethod(method, lambdaIntegration);
    return { lambda, apiResource };
  }

  private setupDynamoDbTable(): ITable {
    const table = new Table(this, 'LearningDynamoDb', {
      tableName: `LearningDynamoDb-${this.props.deploymentEnv}`,
      partitionKey: { name: 'pk', type: AttributeType.STRING },
      sortKey: { name: 'sk', type: AttributeType.STRING },
      removalPolicy: RemovalPolicy.DESTROY,
    });

    table.addGlobalSecondaryIndex({
      indexName: 'gsi1',
      partitionKey: { name: 'gsi1pk', type: AttributeType.STRING },
      sortKey: { name: 'gsi1sk', type: AttributeType.STRING },
      projectionType: ProjectionType.INCLUDE,
      nonKeyAttributes: ['pk', 'Data'],
    });

    return table;
  }

  private setupApiGateway(): apigateway.RestApi {
    const api = new apigateway.RestApi(this, 'learning-dynamodb', {
      defaultCorsPreflightOptions: {
        allowOrigins: apigateway.Cors.ALL_ORIGINS,
        allowMethods: apigateway.Cors.ALL_METHODS
      }
    });
    api.root.addMethod('ANY');
    return api;
  }

  private defineJavaQuarkusLambda(lambdaGroup: string, props: LambdaProps): lambda.Function {
    const defaultJavaEnv = {
      JAVA_TOOL_OPTIONS: `-Dquarkus.lambda.handler=${props.handlerName}`,
      QUARKUS_LAMBDA_HANDLER: props.handlerName,
      DISABLE_SIGNAL_HANDLERS: 'true',
    };
    const environment = Object.assign(defaultJavaEnv, this.getCommonEnvVars(props));
    const lambdaName = `${lambdaGroup}-${props.handlerName}`;
    const funcProps: lambda.FunctionProps = {
      environment,
      timeout: props.timeout || DEFAULT_LAMBDA_TIMEOUT,
      handler: QUARKUS_HANDLER,
      functionName: `${lambdaName}-${this.props.deploymentEnv}`,
      role: props.lambdaRole,
      code: new lambda.AssetCode(`${this.props.javaLambdaPath}/${lambdaGroup}/build/function.zip`),
      runtime: lambda.Runtime.JAVA_11,
      memorySize: props.memorySize,
    };

    const func = new lambda.Function(this, lambdaName, funcProps);
    return this.configureLambda(func, props);
  }

  private configureLambda(func: lambda.Function, props: LambdaProps) {
    if (props.policyStatements) {
      for (const statement of props.policyStatements) {
        func.addToRolePolicy(statement);
      }
    }

    return func;
  }

  private getCommonEnvVars(props: LambdaProps): Record<string, string> {
    const envVars = Object.assign({}, props.environment);
    envVars.ENV = this.props.deploymentEnv;
    envVars.ACCOUNT = this.account;
    return envVars;
  }

  private buildPolicyStatement(actions: string[], resources: string[]): PolicyStatement {
    return new PolicyStatement({
      actions,
      resources,
    });
  }
}
