package com.trilogy.learning.market.requests;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.*;

@Data
@RegisterForReflection
@NoArgsConstructor
public class NewCustomerRequest {
    @NonNull
    String customerEmail;

    @NonNull
    String Name;
}
