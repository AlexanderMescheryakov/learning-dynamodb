package com.trilogy.learning.market.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trilogy.learning.market.model.Product;
import com.trilogy.learning.market.repository.IProductRepository;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.Map;

@Named("update-product")
@AllArgsConstructor
public class UpdateProduct extends AbstractApiGatewayLambda {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private IProductRepository productRepository;

    @Override
    protected String handle(Map<String, String> queryParams, String body) throws JsonProcessingException {
        final var value = objectMapper.readValue(body, Product.class);
        final var product = productRepository.updateProduct(value);
        return objectMapper.writeValueAsString(product);
    }
}
