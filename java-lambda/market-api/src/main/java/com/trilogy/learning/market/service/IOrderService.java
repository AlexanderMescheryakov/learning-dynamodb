package com.trilogy.learning.market.service;

import com.trilogy.learning.market.model.Order;
import com.trilogy.learning.market.requests.NewOrderRequest;

import java.io.IOException;

public interface IOrderService {
    Order addOrder(NewOrderRequest orderRequest) throws IOException;
}
