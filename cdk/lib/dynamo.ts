import { Construct, Duration, RemovalPolicy } from '@aws-cdk/core';
import {
  AttributeType,
  BillingMode,
  CfnTable,
  ITable,
  ProjectionType,
  StreamViewType,
  Table,
} from '@aws-cdk/aws-dynamodb';
import * as cloudwatch from '@aws-cdk/aws-cloudwatch';
import * as cloudwatch_actions from '@aws-cdk/aws-cloudwatch-actions';
import * as sns from '@aws-cdk/aws-sns';
import * as sns_subscriptions from '@aws-cdk/aws-sns-subscriptions';

export interface DynamoDbIndexProps {
  readonly projected: string[];
}

export interface DynamoDbTableProps {
  readonly tableName: string;
  readonly deploymentEnv: string;
  readonly globalIndexes?: DynamoDbIndexProps[];
  readonly removalPolicy?: RemovalPolicy;
  readonly streamType?: StreamViewType;
  readonly billingMode?: BillingMode;
  readonly readCapacityThresholdPercent?: number;
  readonly writeCapacityThresholdPercent?: number;
  readonly alarmEmail?: string;
  readonly alarmSnsTopic?: sns.ITopic;
  readonly alarmSnsTopicName?: string;
}

interface DynamoDbMetricTableDimension {
  readonly TableName: string;
  readonly GlobalSecondaryIndexName?: string;
}

interface CapacityMetrics {
  readCapacityMinuteMetric: cloudwatch.Metric;
  writeCapacityMinuteMetric: cloudwatch.Metric;
  readCapacitySecMetric: cloudwatch.IMetric;
  writeCapacitySecMetric: cloudwatch.IMetric;
}

const PK_NAME = 'PK';
const SK_NAME = 'SK';
const GSI_NAME = 'GSI';
const DYNAMODB_CONSUMPTION_ALARM_PERCENT = 80;
const CAPACITY_ALARM_PERIOD_MINS = 5;

const CONSUMED_SUM_METRIC = {
  label: 'Consumed',
  period: Duration.minutes(1),
  statistic: 'sum',
};

const EVENTS_SUM_METRIC = {
  label: 'Events',
  period: Duration.minutes(1),
  statistic: 'sum',
};

export class DynamoDbTable extends Construct {
  private readonly props: DynamoDbTableProps;
  private readonly table: Table;
  private readonly dashboard: cloudwatch.Dashboard;
  private readonly alarmTopic?: sns.ITopic;

  private gsiNames: string[] = [];

  constructor(scope: Construct, id: string, props: DynamoDbTableProps) {
    super(scope, `${id}Table`);
    this.props = props;
    this.table = this.setupTable(props);
    this.alarmTopic = this.setupAlarmTopic(props);
    this.dashboard = this.setupCloudWatchDashboard();
    this.setupMonitoring(props);
  }

  public getTable(): ITable {
    return this.table;
  }

  private setupTable(props: DynamoDbTableProps): Table {
    const table = new Table(this, `${props.tableName}Table`, {
      tableName: `${props.tableName}-${props.deploymentEnv}`,
      partitionKey: { name: PK_NAME, type: AttributeType.STRING },
      sortKey: { name: SK_NAME, type: AttributeType.STRING },
      removalPolicy: props.removalPolicy || RemovalPolicy.DESTROY,
      stream: props.streamType || StreamViewType.NEW_AND_OLD_IMAGES,
      billingMode: props.billingMode || BillingMode.PROVISIONED,
    });

    if (props.globalIndexes) {
      let gsiNum = 0;
      props.globalIndexes.forEach((gsi) => {
        gsiNum++;
        const indexName = `${GSI_NAME}${gsiNum}`;
        this.gsiNames.push(indexName);
        table.addGlobalSecondaryIndex({
          indexName,
          partitionKey: { name: `${indexName}${PK_NAME}`, type: AttributeType.STRING },
          sortKey: { name: `${indexName}${SK_NAME}`, type: AttributeType.STRING },
          projectionType: ProjectionType.INCLUDE,
          nonKeyAttributes: gsi.projected,
        });
      });
    }

    return table;
  }

  private setupCloudWatchDashboard() {
    const dashboardName = `DynamoDB-${this.props.tableName}-${this.props.deploymentEnv}`;
    return new cloudwatch.Dashboard(this, dashboardName, {
      dashboardName,
    });
  }

  private setupAlarmTopic(props: DynamoDbTableProps): sns.ITopic | undefined {
    let alarmTopic = this.props.alarmSnsTopic;
    if (!alarmTopic && this.props.alarmSnsTopicName) {
      alarmTopic = new sns.Topic(this, `DynamoDB-Alarm-${this.props.deploymentEnv}`, {
        displayName: `DynamoDB-${this.props.tableName}-Alarms`,
        topicName: `${this.props.alarmSnsTopicName}-${this.props.deploymentEnv}`,
      });
    }

    if (props.alarmEmail && alarmTopic) {
      alarmTopic.addSubscription(new sns_subscriptions.EmailSubscription(props.alarmEmail));
    }

    return alarmTopic;
  }

  private addAlarm(alarm: cloudwatch.Alarm) {
    if (this.alarmTopic) {
      alarm.addAlarmAction(new cloudwatch_actions.SnsAction(this.alarmTopic));
    }
  }

  private setupMonitoring(props: DynamoDbTableProps) {
    this.addTableSection(props.tableName);
    const billingMode = props.billingMode || BillingMode.PROVISIONED;
    switch (billingMode) {
      case BillingMode.PAY_PER_REQUEST:
        this.setupMonitoringForPayPerRequestTable();
        break;

      case BillingMode.PROVISIONED:
        this.setupMonitoringForProvisionedTable(props);
        break;
    }

    this.setupTableAndGsiMonitoring(['ReadThrottleEvents', 'WriteThrottleEvents']);
    this.setupTableMonitoring(['TransactionConflict', 'ConditionalCheckFailedRequests']);
  }

  private setupTableMonitoring(metricNames: string[]) {
    const widgets = metricNames.map<cloudwatch.GraphWidget>((name) => {
      const metric = DynamoDbTable.metricForDynamoTable({ TableName: this.table.tableName }, name, EVENTS_SUM_METRIC);
      return DynamoDbTable.createDynamoGraph(name, metric);
    });
    this.dashboard.addWidgets(...widgets);
  }

  private setupTableAndGsiMonitoring(metricNames: string[]) {
    this.setupTableMonitoring(metricNames);

    this.gsiNames.forEach((indexName) => {
      const widgets = metricNames.map<cloudwatch.GraphWidget>((name) => {
        const metric = DynamoDbTable.metricForDynamoTable(
          { TableName: this.table.tableName, GlobalSecondaryIndexName: indexName },
          name,
          EVENTS_SUM_METRIC,
        );
        return DynamoDbTable.createDynamoGraph(`${indexName} ${name}`, metric);
      });

      this.dashboard.addWidgets(...widgets);
    });
  }

  private setupMonitoringForProvisionedTable(props: DynamoDbTableProps) {
    const metrics = this.makeCapacityMetrics();
    const cfnTable = this.table.node.defaultChild as CfnTable;
    const throughput = cfnTable.provisionedThroughput as CfnTable.ProvisionedThroughputProperty;
    const readCapacityThresholdPercent = props.readCapacityThresholdPercent || DYNAMODB_CONSUMPTION_ALARM_PERCENT;
    const writeCapacityThresholdPercent = props.writeCapacityThresholdPercent || DYNAMODB_CONSUMPTION_ALARM_PERCENT;

    this.dashboard.addWidgets(
      DynamoDbTable.createDynamoProvisionedGraph(
        'Read',
        metrics.readCapacitySecMetric,
        throughput.readCapacityUnits,
        readCapacityThresholdPercent,
      ),
      DynamoDbTable.createDynamoProvisionedGraph(
        'Write',
        metrics.writeCapacitySecMetric,
        throughput.writeCapacityUnits,
        writeCapacityThresholdPercent,
      ),
    );

    const readAlarm = this.createDynamoCapacityAlarm(
      'read',
      metrics.readCapacityMinuteMetric,
      throughput.readCapacityUnits,
      readCapacityThresholdPercent,
    );
    this.addAlarm(readAlarm);

    const writeAlarm = this.createDynamoCapacityAlarm(
      'write',
      metrics.writeCapacityMinuteMetric,
      throughput.writeCapacityUnits,
      writeCapacityThresholdPercent,
    );
    this.addAlarm(writeAlarm);
  }

  private setupMonitoringForPayPerRequestTable() {
    const metrics = this.makeCapacityMetrics();
    this.dashboard.addWidgets(
      DynamoDbTable.createDynamoCapacityGraph('Read', metrics.readCapacitySecMetric),
      DynamoDbTable.createDynamoCapacityGraph('Write', metrics.writeCapacitySecMetric),
    );
  }

  private makeCapacityMetrics(): CapacityMetrics {
    const readCapacityMinuteMetric = DynamoDbTable.metricForDynamoTable(
      { TableName: this.table.tableName },
      'ConsumedReadCapacityUnits',
      CONSUMED_SUM_METRIC,
    );
    const writeCapacityMinuteMetric = DynamoDbTable.metricForDynamoTable(
      { TableName: this.table.tableName },
      'ConsumedWriteCapacityUnits',
      CONSUMED_SUM_METRIC,
    );

    const readCapacitySecMetric = DynamoDbTable.averageMetricExpression(
      readCapacityMinuteMetric,
      CONSUMED_SUM_METRIC.period,
    );
    const writeCapacitySecMetric = DynamoDbTable.averageMetricExpression(
      writeCapacityMinuteMetric,
      CONSUMED_SUM_METRIC.period,
    );

    return {
      readCapacityMinuteMetric,
      writeCapacityMinuteMetric,
      readCapacitySecMetric,
      writeCapacitySecMetric,
    };
  }

  private static createDynamoProvisionedGraph(
    type: string,
    metric: cloudwatch.IMetric,
    provisioned: number,
    percent: number,
  ) {
    return new cloudwatch.GraphWidget({
      title: `${type} Capacity Units/sec`,
      width: 12,
      stacked: true,
      left: [metric],
      leftAnnotations: [
        {
          label: 'Provisioned',
          value: provisioned,
          color: '#58D68D',
        },
        {
          label: `Alarm on ${percent}%`,
          value: provisioned * (percent / 100),
          color: '#FF3333',
        },
      ],
    });
  }

  private static createDynamoCapacityGraph(type: string, metric: cloudwatch.IMetric) {
    return new cloudwatch.GraphWidget({
      title: `${type} Capacity Units/sec`,
      width: 12,
      stacked: true,
      left: [metric],
    });
  }

  private static createDynamoGraph(name: string, metric: cloudwatch.Metric) {
    return new cloudwatch.GraphWidget({
      title: `${name}/${metric.period.toMinutes()}min`,
      width: 12,
      stacked: true,
      left: [metric],
    });
  }

  private createDynamoCapacityAlarm(type: string, metric: cloudwatch.Metric, provisioned: number, percent: number) {
    const threshold = provisioned * (percent / 100) * metric.period.toSeconds();
    const alarm = metric.createAlarm(this, `CapacityAlarm:${type}`, {
      alarmDescription: `at ${threshold}% of ${type} capacity`,
      threshold,
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
      evaluationPeriods: CAPACITY_ALARM_PERIOD_MINS,
      statistic: 'sum',
    });
    return alarm;
  }

  private static averageMetricExpression(metric: cloudwatch.Metric, period: Duration) {
    return new cloudwatch.MathExpression({
      expression: `x/${period.toSeconds()}`,
      usingMetrics: { x: metric },
    });
  }

  private static metricForDynamoTable(
    source: DynamoDbMetricTableDimension,
    metricName: string,
    options: cloudwatch.MetricOptions = {},
  ): cloudwatch.Metric {
    return new cloudwatch.Metric({
      metricName,
      namespace: 'AWS/DynamoDB',
      dimensions: source,
      unit: cloudwatch.Unit.COUNT,
      label: metricName,
      ...options,
    });
  }

  private addTableSection(name: string) {
    const linkTitle = 'DynamoDB Console';
    const linkUrl = this.getConsoleLink();
    const markdown = [`# Table: ${name}`, `[button:${linkTitle}](${linkUrl})`];
    this.dashboard.addWidgets(new cloudwatch.TextWidget({ width: 24, markdown: markdown.join('\n') }));
  }

  private getConsoleLink(): string {
    const region = this.table.stack.region;
    const name = this.table.tableName;
    const tab = 'overview';
    return `https://console.aws.amazon.com/dynamodb/home?region=${region}#tables:selected=${name};tab=${tab}`;
  }
}
