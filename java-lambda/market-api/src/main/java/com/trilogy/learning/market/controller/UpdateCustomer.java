package com.trilogy.learning.market.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trilogy.learning.market.repository.ICustomerRepository;
import com.trilogy.learning.market.requests.UpdateCustomerRequest;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.Map;

@Named("update-customer")
@AllArgsConstructor
public class UpdateCustomer extends AbstractApiGatewayLambda {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private ICustomerRepository customerRepository;

    @Override
    protected String handle(Map<String, String> queryParams, String body) throws JsonProcessingException {
        final var request = objectMapper.readValue(body, UpdateCustomerRequest.class);
        final var customer = customerRepository.updateCustomer(request);
        return objectMapper.writeValueAsString(customer);
    }
}
