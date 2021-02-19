package com.trilogy.learning.market.repository;

import com.trilogy.learning.market.model.Order;

import java.util.List;

public interface IOrderRepository {
    Order getById(String id);

    List<Order> getByCustomerEmail(String email);
}
