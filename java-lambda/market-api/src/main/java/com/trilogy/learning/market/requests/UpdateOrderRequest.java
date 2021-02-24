package com.trilogy.learning.market.requests;

import com.trilogy.learning.market.model.Order;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.*;

@Data
@NoArgsConstructor
@RegisterForReflection
public class UpdateOrderRequest {
    @NonNull
    String id;

    Order.Status status;
}
