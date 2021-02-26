package com.trilogy.learning.market.repository.dynamodb.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;

@Data
@AllArgsConstructor
@NoArgsConstructor
@RegisterForReflection
public class Metadata {
    private static final String ENTITY_TYPE_ATTRIBUTE = "EntityType";
    private static final StaticTableSchema<Metadata> TABLE_SCHEMA =
            StaticTableSchema.builder(Metadata.class)
                    .newItemSupplier(Metadata::new)
                    .addAttribute(EntityType.class, a -> a.name(ENTITY_TYPE_ATTRIBUTE)
                            .getter(Metadata::getEntityType)
                            .setter(Metadata::setEntityType))
                    .build();

    @JsonIgnore
    private EntityType entityType;

    public static StaticTableSchema<Metadata> getSchema() {
        return TABLE_SCHEMA;
    }

    public static String getEntityTypeAttributeName() { return ENTITY_TYPE_ATTRIBUTE; }
}
