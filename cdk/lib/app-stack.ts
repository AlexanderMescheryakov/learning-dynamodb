import { App, RemovalPolicy, Stack, StackProps } from '@aws-cdk/core';
import { AttributeType, ProjectionType, Table } from '@aws-cdk/aws-dynamodb';

export interface AppStackProps extends StackProps {
  deploymentEnv: string;
}

export class AppStack extends Stack {
  private readonly props: AppStackProps;

  public constructor(app: App, id: string, props: AppStackProps) {
    super(app, id, props);
    this.props = props;

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
  }
}
