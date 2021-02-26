package com.trilogy.learning.market.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Payment {
    public enum Status {
        SUCCESS,
        SKIPPED,
        NOT_ALLOWERED
    }

    private String customerId;
    private LocalDateTime date;
    private BigDecimal amount;
}
