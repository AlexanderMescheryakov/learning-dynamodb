package com.trilogy.learning.market.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder(toBuilder=true)
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Order {
    public enum Status {
        OPEN,
        DELIVERED,
        PAID
    }

    String id;

    Status status;

    BigDecimal total;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Date deliveredAt;

    String customerEmail;

    List<OrderedProduct> products;
}
