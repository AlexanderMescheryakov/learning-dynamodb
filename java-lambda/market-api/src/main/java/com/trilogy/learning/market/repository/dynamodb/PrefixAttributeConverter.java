package com.trilogy.learning.market.repository.dynamodb;

import lombok.AllArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@AllArgsConstructor
public class PrefixAttributeConverter implements AttributeConverter<String> {
    private final String prefix;

    @Override
    public AttributeValue transformFrom(String input) {
        return AttributeValue.builder().s(prefix + input).build();
    }

    @Override
    public String transformTo(AttributeValue input) {
        var parts = input.s().split(AbstractRepository.SEP);
        if (parts.length > 1) {
            return parts[1];
        }

        return "";
    }

    @Override
    public EnhancedType<String> type() {
        return EnhancedType.of(String.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}