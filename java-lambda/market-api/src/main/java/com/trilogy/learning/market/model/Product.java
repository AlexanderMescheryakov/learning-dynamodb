package com.trilogy.learning.market.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@RegisterForReflection
public class Product {
    String name;
    BigDecimal price;
    Integer quantity;
}
