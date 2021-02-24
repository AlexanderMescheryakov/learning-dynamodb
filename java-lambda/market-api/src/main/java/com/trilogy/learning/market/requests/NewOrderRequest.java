package com.trilogy.learning.market.requests;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.*;

import java.util.Map;

@Data
@RegisterForReflection
@NoArgsConstructor
public class NewOrderRequest {
    @NonNull
    String customerEmail;

    @NonNull
    Map<String, NewOrderedProductRequest> products;
}
