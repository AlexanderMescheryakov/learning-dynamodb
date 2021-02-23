package com.trilogy.learning.market.requests;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
@RegisterForReflection
public class NewOrderedProduct {
    @NonNull
    Integer quantity;
}
