package com.trilogy.learning.market.service;

import lombok.extern.jbosslog.JBossLog;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

@JBossLog
@Singleton
public class RestrictedDynamoDbService {
    private static final String DEFAULT_REGION = "us-east-1";
    public static final String READ_POLICY =
                    "{" +
                    "  \"Version\": \"2012-10-17\"," +
                    "  \"Statement\": [" +
                    "    {" +
                    "      \"Effect\": \"Allow\"," +
                    "      \"Action\": [" +
                    "         \"dynamodb:GetItem\"," +
                    "         \"dynamodb:Query\"," +
                    "         \"dynamodb:BatchGetItem\"" +
                    "       ]," +
                    "      \"Resource\": [\"arn:aws:dynamodb:*:*:table/*\"]," +
                    "      \"Condition\": {" +
                    "        \"ForAllValues:StringLike\": {" +
                    "          \"dynamodb:LeadingKeys\": [\"*#%s\"]" +
                    "         }" +
                    "       }" +
                    "    }" +
                    "  ]" +
                    "}";

    private final String regionName = Optional.ofNullable(System.getenv("AWS_REGION")).orElse(DEFAULT_REGION);
    private final String roleArn = Optional.ofNullable(System.getenv("ROLE_ARN")).orElseThrow();
    private final StsClient stsClient;

    private Map<String, DynamoDbClient> clients = new HashMap<>();
    private DynamoDbClient currentClient;

    public RestrictedDynamoDbService() {
        stsClient = StsClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }

    public void setAuthId(String id) {
        if (!clients.containsKey(id)) {
            final var policy = format(READ_POLICY, id);
            log.info(policy);
            log.info(roleArn);
            final var assumeRoleRequest = AssumeRoleRequest.builder()
                    .roleArn(roleArn)
                    .roleSessionName("DynamoDbByUser" + id)
                    .policy(policy)
                    .build();
            final var assumeRoleResponse = stsClient.assumeRole(assumeRoleRequest);
            log.info(assumeRoleResponse);
            final var responseCredentials = assumeRoleResponse.credentials();
            final var credentials = AwsSessionCredentials.create(responseCredentials.accessKeyId(),
                    responseCredentials.secretAccessKey(), responseCredentials.sessionToken());
            final var provider = StaticCredentialsProvider.create(credentials);
            log.info(provider);
            currentClient = DynamoDbClient.builder()
                    .region(Region.of(regionName))
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .credentialsProvider(provider)
                    .build();
            clients.put(id, currentClient);
            return;
        }

        currentClient = clients.get(id);
    }

    public DynamoDbClient getClient() {
        if (currentClient == null) {
            throw new NullPointerException("currentClient");
        }

        return currentClient;
    }
}
