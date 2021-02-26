package com.trilogy.learning.market.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trilogy.learning.market.repository.IPaymentRepository;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.Map;

@Named("get-payments")
@AllArgsConstructor
public class GetPayments extends AbstractApiGatewayLambda {
    private static final String CUSTOMER_ID_PARAM = "customerId";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private IPaymentRepository paymentRepository;

    @Override
    protected String handle(Map<String, String> queryParams, String body) throws JsonProcessingException {
        if (queryParams.containsKey(CUSTOMER_ID_PARAM)) {
            final var id = queryParams.get(CUSTOMER_ID_PARAM);
            final var items = paymentRepository.getByCustomerId(id);
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return objectMapper.writeValueAsString(items);
        }

        return null;
    }
}
