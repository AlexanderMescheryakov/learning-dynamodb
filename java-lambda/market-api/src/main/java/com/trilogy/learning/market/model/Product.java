package com.trilogy.learning.market.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Product {
    private String id;
    private String name;
    private BigDecimal price;
    private String category;
}
