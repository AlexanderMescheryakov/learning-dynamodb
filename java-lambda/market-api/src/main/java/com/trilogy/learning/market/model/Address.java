package com.trilogy.learning.market.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@RegisterForReflection
public class Address {
    String country;
    String city;
    String streetAddress;
}
