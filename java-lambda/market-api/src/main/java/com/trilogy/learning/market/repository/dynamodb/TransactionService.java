package com.trilogy.learning.market.repository.dynamodb;

import com.trilogy.learning.market.repository.ITransactionService;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse;

import javax.inject.Singleton;
import java.util.Collection;

@Singleton
@AllArgsConstructor
public class TransactionService implements ITransactionService {
    protected final DynamoDbClient dynamoDbClient;

    @Override
    public TransactWriteItemsResponse commit(Collection<TransactWriteItem> requests) {
        final var request = TransactWriteItemsRequest.builder().transactItems(requests).build();
        return dynamoDbClient.transactWriteItems(request);
    }
}
