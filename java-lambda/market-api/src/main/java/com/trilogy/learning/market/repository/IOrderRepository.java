package com.trilogy.learning.market.repository;

import com.trilogy.learning.market.model.Order;

import java.util.List;

public interface IOrderRepository {
    Order getById(String id);

    Order getByIdDetails(String id);

    List<Order> getByCustomerEmail(String email);

    public void addOrder(Order order);

    public void updateOrder(Order order);

    public void deleteOrder(String id);
}
