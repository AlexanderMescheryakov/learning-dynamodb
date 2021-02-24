package com.trilogy.learning.market.requests;

import com.trilogy.learning.market.model.Address;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.*;

@Data
@NoArgsConstructor
@RegisterForReflection
public class UpdateCustomerRequest {
    @NonNull
    String id;

    String name;

    Address address;
}
