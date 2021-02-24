package com.trilogy.learning.market.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trilogy.learning.market.model.Order;
import com.trilogy.learning.market.repository.IOrderRepository;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.Map;

@Named("get-orders")
@AllArgsConstructor
public class GetOrders extends AbstractApiGatewayLambda {
    private static final String CUSTOMER_ID_PARAM = "customerId";
    private static final String PRODUCT_ID_PARAM = "productId";
    private static final String STATUS_ID_PARAM = "status";
    private static final String MONTH_ID_PARAM = "month";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private IOrderRepository orderRepository;

    @Override
    protected String handle(Map<String, String> queryParams, String body) throws JsonProcessingException {
        if (queryParams.containsKey(CUSTOMER_ID_PARAM)) {
            final var customerId = queryParams.get(CUSTOMER_ID_PARAM);
            final var orders = orderRepository.getByCustomerEmail(customerId);
            return objectMapper.writeValueAsString(orders);
        } else if (queryParams.containsKey(PRODUCT_ID_PARAM)) {
            final var productId = queryParams.get(PRODUCT_ID_PARAM);
            final var orders = orderRepository.getByProductId(productId);
            return objectMapper.writeValueAsString(orders);
        } else if (queryParams.containsKey(STATUS_ID_PARAM)) {
            final var statusStr = queryParams.get(STATUS_ID_PARAM).toUpperCase();
            var month = "";
            if (queryParams.containsKey(MONTH_ID_PARAM)) {
                month = queryParams.get(MONTH_ID_PARAM);
            }

            final var status = Order.Status.valueOf(statusStr);
            final var orders = orderRepository.getByStatus(status, month);
            return objectMapper.writeValueAsString(orders);
        }

        return null;
    }
}
