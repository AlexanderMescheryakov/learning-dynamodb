package com.trilogy.learning.market.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Order {
    public enum Status {
        OPEN,
        DELIVERED,
    }

    String id;
    Status status;
    BigDecimal total;
    Date createdAt;
    Date deliveredAt;
    String customerEmail;
    List<OrderedProduct> products;
}
