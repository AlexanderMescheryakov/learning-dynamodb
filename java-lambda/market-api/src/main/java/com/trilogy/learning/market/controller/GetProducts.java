package com.trilogy.learning.market.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trilogy.learning.market.repository.IProductRepository;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.Map;

@Named("get-products")
@AllArgsConstructor
public class GetProducts extends AbstractApiGatewayLambda {
    private static final String ID_PARAM = "category";
    private static final String OUT_OF_STOCK_PARAM = "outOfStock";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private IProductRepository productRepository;

    @Override
    protected String handle(Map<String, String> queryParams, String body) throws JsonProcessingException {
        if (queryParams.containsKey(ID_PARAM)) {
            final var id = queryParams.get(ID_PARAM);
            final var items = productRepository.getByCategory(id);
            return objectMapper.writeValueAsString(items);
        } else if (queryParams.containsKey(OUT_OF_STOCK_PARAM)) {
            final var items = productRepository.getIfOutOfStock();
            return objectMapper.writeValueAsString(items);
        }

        return null;
    }
}
