package com.trilogy.learning.market.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trilogy.learning.market.serialization.DateModule;
import io.quarkus.jackson.ObjectMapperCustomizer;
import javax.inject.Singleton;

@Singleton
public class QuarkusModuleCustomizer implements ObjectMapperCustomizer {

    public void customize(ObjectMapper mapper) {
        mapper.registerModule(new DateModule());
    }
}
