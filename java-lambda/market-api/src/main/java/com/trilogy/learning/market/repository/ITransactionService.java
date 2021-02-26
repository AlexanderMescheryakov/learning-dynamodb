package com.trilogy.learning.market.repository;

import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse;

import java.util.Collection;

public interface ITransactionService {
    TransactWriteItemsResponse commit(Collection<TransactWriteItem> requests);
}
