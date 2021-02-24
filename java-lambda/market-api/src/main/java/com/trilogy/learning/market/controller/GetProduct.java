package com.trilogy.learning.market.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trilogy.learning.market.repository.IProductRepository;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.Map;

@Named("get-product")
@AllArgsConstructor
public class GetProduct extends AbstractApiGatewayLambda {
    private static final String ID_PARAM = "id";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private IProductRepository productRepository;

    @Override
    protected String handle(Map<String, String> queryParams, String body) throws JsonProcessingException {
        if (queryParams.containsKey(ID_PARAM)) {
            final var id = queryParams.get(ID_PARAM);
            final var item = productRepository.getById(id);
            return objectMapper.writeValueAsString(item);
        }

        return null;
    }
}
