package com.trilogy.learning.market.controller;

import com.trilogy.learning.market.repository.ICustomerRepository;
import com.trilogy.learning.market.repository.IOrderRepository;
import com.trilogy.learning.market.repository.dynamodb.entity.EntityType;
import lombok.AllArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.OperationType;
import com.trilogy.learning.market.repository.dynamodb.entity.Metadata;

import javax.inject.Named;
import java.io.IOException;
import java.util.Map;

@Named("dynamodb-stream-handler")
@JBossLog
@AllArgsConstructor
public class DynamoStreamHandler extends AbstractDynamoStreamLambda {
    private ICustomerRepository customerRepository;
    private IOrderRepository orderRepository;

    @Override
    protected void handle(Map<String, AttributeValue> oldItem, Map<String,
            AttributeValue> newItem, OperationType operationType) throws IOException {
        final var entityType = getEntityType(newItem);
        if (entityType == EntityType.Order) {
            final var order = orderRepository.getOrderFromItem(newItem);
            switch (operationType) {
                case INSERT:
                    customerRepository.incrementOrderCount(order.getCustomerEmail(), 1);
                    break;
                case REMOVE:
                    customerRepository.incrementOrderCount(order.getCustomerEmail(), -1);
            }
        }
    }

    private EntityType getEntityType(Map<String, AttributeValue> item) {
        try {
            log.info(item);
            final var meta = Metadata.getSchema().mapToItem(item);
            if (meta != null) {
                return meta.getEntityType();
            }

            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
