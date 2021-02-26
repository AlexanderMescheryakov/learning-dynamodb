package com.trilogy.learning.market.repository;

import com.trilogy.learning.market.model.Order;
import com.trilogy.learning.market.requests.UpdateOrderRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public interface IOrderRepository {
    Order getById(String id);

    Order getByIdDetails(String id);

    List<Order> getByCustomerEmail(String email);

    List<Order> getByProductId(String productId);

    List<Order> getByStatus(Order.Status status, String month);

    void addOrder(Order order);

    Order updateOrder(UpdateOrderRequest order);

    Order deleteOrder(String id);

    Order getOrderFromItem(Map<String, AttributeValue> item);
}
