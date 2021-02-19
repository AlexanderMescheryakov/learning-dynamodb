package com.trilogy.learning.market.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@RegisterForReflection
public class Address {
    String country;
    String city;
    String streetAddress;
}
