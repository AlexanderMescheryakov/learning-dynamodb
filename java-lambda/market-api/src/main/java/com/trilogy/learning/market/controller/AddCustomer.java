package com.trilogy.learning.market.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trilogy.learning.market.model.Customer;
import com.trilogy.learning.market.repository.ICustomerRepository;
import com.trilogy.learning.market.requests.NewCustomerRequest;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import javax.inject.Named;
import java.util.Map;

@Named("add-customer")
@AllArgsConstructor
public class AddCustomer extends AbstractApiGatewayLambda {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private ICustomerRepository customerRepository;

    @Override
    protected String handle(Map<String, String> queryParams, String body) throws JsonProcessingException {
        final var request = objectMapper.readValue(body, NewCustomerRequest.class);
        final var customer = Customer.builder()
                .email(request.getCustomerEmail())
                .name(request.getName())
                .build();
        customerRepository.addCustomer(customer);
        return objectMapper.writeValueAsString(customer);
    }
}
