package com.trilogy.learning.market.controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trilogy.learning.market.repository.IOrderRepository;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import javax.inject.Named;
import java.net.HttpURLConnection;
import java.util.TreeMap;

@Named("get-orders")
@AllArgsConstructor
public class GetOrders implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final String CUSTOMER_ID_PARAM = "customerId";

    private IOrderRepository orderRepository;

    @SneakyThrows
    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        final var params = input.getQueryStringParameters();
        if (params != null) {
            final var caseInsensitiveParams = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            caseInsensitiveParams.putAll(params);
            if (caseInsensitiveParams.containsKey(CUSTOMER_ID_PARAM)) {
                final var customerId = params.get(CUSTOMER_ID_PARAM);
                final var orders = orderRepository.getByCustomerEmail(customerId);
                final var objectMapper = new ObjectMapper();
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(HttpURLConnection.HTTP_OK)
                        .withBody(objectMapper.writeValueAsString(orders));
            }
        }

        return new APIGatewayProxyResponseEvent().withStatusCode(HttpURLConnection.HTTP_NOT_FOUND);
    }
}
