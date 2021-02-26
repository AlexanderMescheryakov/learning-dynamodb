package com.trilogy.learning.market.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trilogy.learning.market.requests.NewPaymentRequest;
import com.trilogy.learning.market.service.IPaymentService;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.Map;

@Named("add-payment")
@AllArgsConstructor
public class AddPayment extends AbstractApiGatewayLambda {
    private IPaymentService paymentService;

    @Override
    protected String handle(Map<String, String> queryParams, String body) throws JsonProcessingException {
        final var objectMapper = new ObjectMapper();
        final var request = objectMapper.readValue(body, NewPaymentRequest.class);
        final var result = paymentService.handleOrderPayment(request.getCustomerId(), request.getOrderId(), request.getAmount());
        return objectMapper.writeValueAsString(Map.of("result", result));
    }
}
