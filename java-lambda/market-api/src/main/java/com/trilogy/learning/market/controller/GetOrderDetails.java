package com.trilogy.learning.market.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trilogy.learning.market.repository.IOrderRepository;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.Map;

@Named("get-order")
@AllArgsConstructor
public class GetOrderDetails extends AbstractApiGatewayLambda {
    private static final String ID_PARAM = "id";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private IOrderRepository orderRepository;

    @Override
    protected String handle(Map<String, String> queryParams, String body) throws JsonProcessingException {
        if (queryParams.containsKey(ID_PARAM)) {
            final var customerId = queryParams.get(ID_PARAM);
            final var order = orderRepository.getByIdDetails(customerId);
            return objectMapper.writeValueAsString(order);
        }

        return null;
    }
}
