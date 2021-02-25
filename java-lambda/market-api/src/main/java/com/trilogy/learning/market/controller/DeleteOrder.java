package com.trilogy.learning.market.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trilogy.learning.market.repository.IOrderRepository;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.Map;

@Named("delete-order")
@AllArgsConstructor
public class DeleteOrder extends AbstractApiGatewayLambda {
    private static final String ID_PARAM = "id";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final IOrderRepository orderRepository;

    @Override
    protected String handle(Map<String, String> queryParams, String body) throws JsonProcessingException {
        if (queryParams.containsKey(ID_PARAM)) {
            final var id = queryParams.get(ID_PARAM);
            final var order = orderRepository.deleteOrder(id);
            return objectMapper.writeValueAsString(order);
        }

        return null;
    }
}
