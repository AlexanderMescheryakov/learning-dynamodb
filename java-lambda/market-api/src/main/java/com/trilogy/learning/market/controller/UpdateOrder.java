package com.trilogy.learning.market.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trilogy.learning.market.repository.IOrderRepository;
import com.trilogy.learning.market.requests.UpdateOrderRequest;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.Map;

@Named("update-order")
@AllArgsConstructor
public class UpdateOrder extends AbstractApiGatewayLambda {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private IOrderRepository orderRepository;

    @Override
    protected String handle(Map<String, String> queryParams, String body) throws JsonProcessingException {
        final var request = objectMapper.readValue(body, UpdateOrderRequest.class);
        final var order = orderRepository.updateOrder(request);
        return objectMapper.writeValueAsString(order);
    }
}
