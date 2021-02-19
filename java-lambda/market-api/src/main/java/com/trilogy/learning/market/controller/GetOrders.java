package com.trilogy.learning.market.controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.trilogy.learning.market.model.Order;
import com.trilogy.learning.market.repository.IOrderRepository;
import lombok.AllArgsConstructor;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@Named("get-orders")
@AllArgsConstructor
public class GetOrders implements RequestHandler<APIGatewayProxyRequestEvent, List<Order>> {
    private static final String CUSTOMER_ID_PARAM = "customerId";

    private IOrderRepository orderRepository;

    @Override
    public List<Order> handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        final var params = input.getPathParameters();
        if (params != null) {
            final var caseInsensitiveParams = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            caseInsensitiveParams.putAll(params);
            if (caseInsensitiveParams.containsKey(CUSTOMER_ID_PARAM)) {
                final var customerId = params.get(CUSTOMER_ID_PARAM);
                return orderRepository.getByCustomerEmail(customerId);
            }
        }

        return new ArrayList<Order>();
    }
}
