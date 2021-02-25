package com.trilogy.learning.market.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trilogy.learning.market.requests.NewOrderRequest;
import com.trilogy.learning.market.service.IOrderService;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.io.IOException;
import java.util.Map;

@Named("add-order")
@AllArgsConstructor
public class AddOrder extends AbstractApiGatewayLambda {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private IOrderService orderService;

    @Override
    protected String handle(Map<String, String> queryParams, String body) throws IOException {
        final var request = objectMapper.readValue(body, NewOrderRequest.class);
        var order = orderService.addOrder(request);
        return objectMapper.writeValueAsString(order);
    }
}
