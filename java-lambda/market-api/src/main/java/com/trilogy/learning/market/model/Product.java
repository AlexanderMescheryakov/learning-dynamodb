package com.trilogy.learning.market.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.trilogy.learning.market.repository.dynamodb.entity.Metadata;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Optional;

@Data
@AllArgsConstructor
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Product extends Metadata {
    static final String OUT_OF_STOCK_MARKER = "OUT_OF_STOCK";

    public Product() {
        outOfStock = false;
    }

    private String id;
    private String name;
    private BigDecimal price;
    private String category;
    private Boolean outOfStock;

    @JsonIgnore
    public String getOutOfStockMarker() {
        return Optional.ofNullable(getOutOfStock()).orElse(false) ? OUT_OF_STOCK_MARKER : null;
    }

    public void setOutOfStockMarker(String marker) {
        setOutOfStock(OUT_OF_STOCK_MARKER.equals(marker) ? true : false);
    }

    @JsonIgnore
    public String getOutOfStockName() {
        return Optional.ofNullable(getOutOfStock()).orElse(false) ? name : null;
    }

    public void setOutOfStockName(String name) {
        if (name != null) {
            setName(name);
        }
    }
}
