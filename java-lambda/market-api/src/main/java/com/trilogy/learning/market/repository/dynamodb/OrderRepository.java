package com.trilogy.learning.market.repository.dynamodb;

import com.trilogy.learning.market.model.Order;
import com.trilogy.learning.market.repository.IOrderRepository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Singleton
public class OrderRepository extends AbstractRepository<Order> implements IOrderRepository {
    private static final String CUSTOMER_EMAIL_ATTRIBUTE = "CustomerEmail";
    private static final String DATE_DELIVERED_ATTRIBUTE = "DateDelivered";
    private static final String KEY_PREFIX = "ORDER#";

    @Inject
    public OrderRepository(DynamoDbClient dynamoDbClient) {
        super(dynamoDbClient);
    }

    @Override
    public Order getById(String id) {
        final var response = getItem(id);
        return getOrderFromResponse(response, id);
    }

    @Override
    public List<Order> getByCustomerEmail(String email) {
        final var response = queryParent(PK_ATTRIBUTE, CustomerRepository.KEY_PREFIX + email,
                SK_ATTRIBUTE, KEY_PREFIX);
        return getOrdersFromResponse(response, email);
    }

    private Order getOrderFromResponse(GetItemResponse response, String id) {
        return Order.builder()
                .id(id)
                .createdAt(getDateOrDefault(response, GSI1_SK_ATTRIBUTE, null))
                .deliveredAt(getDateOrDefault(response, DATE_DELIVERED_ATTRIBUTE, null))
                .customerEmail(response.getValueForField(CUSTOMER_EMAIL_ATTRIBUTE, String.class).orElse(""))
                .build();
    }

    private List<Order> getOrdersFromResponse(QueryResponse response, String email) {
        final var orders = new ArrayList<Order>();
        if (!response.hasItems()) {
            return orders;
        }

        final var items = response.items();
        for (final var item : items) {
            final var order = getOrderFromAttributeValue(item, email);
            if (order != null) {
                orders.add(order);
            }
        }

        return orders;
    }

    private Order getOrderFromAttributeValue(Map<String, AttributeValue> value, String email) {
        final var orderStatusAndId = value.get(GSI1_SK_ATTRIBUTE).s();
        var parts = orderStatusAndId.split("#");
        if (parts.length < 3) {
            return null;
        }

        return Order.builder()
                .status(Order.Status.valueOf(parts[1]))
                .id(parts[2])
                .createdAt(getDateOrDefault(value, GSI1_SK_ATTRIBUTE, null))
                .deliveredAt(getDateOrDefault(value, DATE_DELIVERED_ATTRIBUTE, null))
                .customerEmail(email)
                .build();
    }

    @Override
    protected String getKeyPrefix() {
        return KEY_PREFIX;
    }
}
