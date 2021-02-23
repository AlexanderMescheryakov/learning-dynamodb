package com.trilogy.learning.market.service;

import com.trilogy.learning.market.requests.NewOrder;

import java.io.IOException;

public interface IOrderService {
    void addOrder(NewOrder orderRequest) throws IOException;
}
