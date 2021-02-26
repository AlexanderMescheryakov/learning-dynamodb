package com.trilogy.learning.market.requests;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.*;

import java.math.BigDecimal;

@Data
@RegisterForReflection
@NoArgsConstructor
public class NewPaymentRequest {
    @NonNull
    String customerId;

    @NonNull
    String orderId;

    @NonNull
    BigDecimal amount;
}
