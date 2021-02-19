package com.trilogy.learning.market.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@RegisterForReflection
public class Order {
    public enum Status {
        CREATED,
        DELIVERED,
    }

    String id;
    Status status;
    Date createdAt;
    Date deliveredAt;
    String customerEmail;
}
