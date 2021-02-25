package com.trilogy.learning.market.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trilogy.learning.market.repository.IProductRepository;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.Map;

@Named("delete-product")
@AllArgsConstructor
public class DeleteProduct extends AbstractApiGatewayLambda {
    private static final String ID_PARAM = "id";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private IProductRepository productRepository;

    @Override
    protected String handle(Map<String, String> queryParams, String body) throws JsonProcessingException {
        if (queryParams.containsKey(ID_PARAM)) {
            final var id = queryParams.get(ID_PARAM);
            final var product = productRepository.deleteProduct(id);
            return objectMapper.writeValueAsString(product);
        }

        return null;
    }
}
