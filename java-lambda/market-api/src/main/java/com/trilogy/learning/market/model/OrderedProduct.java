package com.trilogy.learning.market.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@RegisterForReflection
public class OrderedProduct {
    String id;
    String orderId;
    String name;
    BigDecimal price;
    Integer quantity;

    public static BigDecimal getTotal(List<OrderedProduct> orderedProducts) {
        return orderedProducts.stream()
                .map(x -> x.getPrice().multiply(BigDecimal.valueOf(x.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
