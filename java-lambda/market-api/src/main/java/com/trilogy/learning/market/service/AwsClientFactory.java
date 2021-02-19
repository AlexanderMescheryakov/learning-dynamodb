package com.trilogy.learning.market.service;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.util.Optional;

@ApplicationScoped
public class AwsClientFactory {
    private static final String DEFAULT_REGION = "us-east-1";

    private final String regionName = Optional.ofNullable(System.getenv("AWS_REGION")).orElse(DEFAULT_REGION);

    @Produces
    public DynamoDbClient getDynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.of(regionName))
                .build();
    }
}
