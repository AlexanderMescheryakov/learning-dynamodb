package com.trilogy.learning.market.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trilogy.learning.market.repository.ICustomerRepository;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.Map;

@Named("get-customer")
@AllArgsConstructor
public class GetCustomer extends AbstractApiGatewayLambda {
    private static final String ID_PARAM = "id";
    private static final String ORDER_ID_PARAM = "orderId";

    private ICustomerRepository customerRepository;

    @Override
    protected String handle(Map<String, String> queryParams, String body) throws JsonProcessingException {
        final var objectMapper = new ObjectMapper();
        if (queryParams.containsKey(ID_PARAM)) {
            final var customerId = queryParams.get(ID_PARAM);
            final var customer = customerRepository.getById(customerId);
            return objectMapper.writeValueAsString(customer);
        } else if (queryParams.containsKey(ORDER_ID_PARAM)) {
            final var orderId = queryParams.get(ORDER_ID_PARAM);
            final var customer = customerRepository.getByOrderId(orderId);
            return objectMapper.writeValueAsString(customer);
        }

        return null;
    }
}
