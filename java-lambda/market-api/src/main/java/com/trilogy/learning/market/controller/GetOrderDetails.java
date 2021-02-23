package com.trilogy.learning.market.controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trilogy.learning.market.model.ErrorMessage;
import com.trilogy.learning.market.repository.IOrderRepository;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import javax.inject.Named;
import java.net.HttpURLConnection;
import java.util.TreeMap;

@Named("get-order")
@AllArgsConstructor
public class GetOrderDetails implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final String ID_PARAM = "id";

    private IOrderRepository orderRepository;

    @SneakyThrows
    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        final var params = input.getQueryStringParameters();
        if (params != null) {
            final var caseInsensitiveParams = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            caseInsensitiveParams.putAll(params);
            if (caseInsensitiveParams.containsKey(ID_PARAM)) {
                final var objectMapper = new ObjectMapper();
                try {
                    final var customerId = params.get(ID_PARAM);
                    final var order = orderRepository.getByIdDetails(customerId);
                    return new APIGatewayProxyResponseEvent()
                            .withStatusCode(HttpURLConnection.HTTP_OK)
                            .withBody(objectMapper.writeValueAsString(order));
                } catch (Exception e) {
                    var errorMessage = objectMapper.writeValueAsString(new ErrorMessage(e));
                    return new APIGatewayProxyResponseEvent()
                            .withStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
                            .withBody(errorMessage);
                }
            }
        }

        return new APIGatewayProxyResponseEvent().withStatusCode(HttpURLConnection.HTTP_NOT_FOUND);
    }
}
