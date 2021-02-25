package com.trilogy.learning.market.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trilogy.learning.market.model.Product;
import com.trilogy.learning.market.repository.IProductRepository;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.Map;

@Named("add-product")
@AllArgsConstructor
public class AddProduct extends AbstractApiGatewayLambda {
    private IProductRepository productRepository;

    @Override
    protected String handle(Map<String, String> queryParams, String body) throws JsonProcessingException {
        final var objectMapper = new ObjectMapper();
        final var value = objectMapper.readValue(body, Product.class);
        productRepository.addProduct(value);
        return objectMapper.writeValueAsString(value);
    }
}
