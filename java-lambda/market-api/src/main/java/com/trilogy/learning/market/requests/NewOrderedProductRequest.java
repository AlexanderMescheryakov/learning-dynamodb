package com.trilogy.learning.market.requests;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.*;

@Data
@RegisterForReflection
@NoArgsConstructor
public class NewOrderedProductRequest {
    @NonNull
    Integer quantity;
}
