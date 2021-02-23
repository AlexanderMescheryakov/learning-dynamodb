package com.trilogy.learning.market.controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trilogy.learning.market.model.ErrorMessage;
import com.trilogy.learning.market.requests.NewOrder;
import com.trilogy.learning.market.service.IOrderService;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import javax.inject.Named;
import java.net.HttpURLConnection;

@Named("add-order")
@AllArgsConstructor
public class AddOrder implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private IOrderService orderService;

    @SneakyThrows
    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        final var objectMapper = new ObjectMapper();
        try {
            final var order = objectMapper.readValue(input.getBody(), NewOrder.class);
            orderService.addOrder(order);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            var errorMessage = objectMapper.writeValueAsString(new ErrorMessage(e));
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
                    .withBody(errorMessage);
        }
    }
}
