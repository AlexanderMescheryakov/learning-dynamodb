package com.trilogy.learning.market.requests;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

@Data
@Builder
@RegisterForReflection
public class NewOrder {
    @NonNull
    String customeEmail;

    @NonNull
    Map<String, NewOrderedProduct> products;
}
