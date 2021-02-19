package com.trilogy.learning.market.repository.dynamodb;

import java.util.*;
import lombok.AllArgsConstructor;
import org.joda.time.DateTime;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@AllArgsConstructor
public abstract class AbstractRepository<T> {
    private static final String TABLE_NAME_ENV_VAR = "DYNAMODB_TABLE";
    private static final String DEFAULT_TABLE_NAME = "Table";
    protected static final String PK_ATTRIBUTE= "pk";
    protected static final String SK_ATTRIBUTE = "sk";
    protected static final String GSI1_PK_ATTRIBUTE= "gsi1pk";
    protected static final String GSI1_SK_ATTRIBUTE = "gsi1sk";
    protected static final String DATA_ATTRIBUTE = "Data";

    private final String tableName = Optional.ofNullable(System.getenv(TABLE_NAME_ENV_VAR)).orElse(DEFAULT_TABLE_NAME);

    protected DynamoDbClient dynamoDbClient;

    public String getTableName() {
        return tableName;
    }

    protected GetItemResponse getItem(String id) {
        final var keyMap = getEntityKeyMap(id);
        final var request = GetItemRequest.builder()
                .tableName(getTableName())
                .key(keyMap)
                .build();
        return dynamoDbClient.getItem(request);
    }

    protected QueryResponse queryParent(String parentKeyAttribute, String parentKey,
                                           String sortAttribute, String sortPrefix) {
        final var keyConditions = new HashMap<String, Condition>();
        keyConditions.put(parentKeyAttribute, getCondition(ComparisonOperator.EQ, parentKey));
        keyConditions.put(sortAttribute, getCondition(ComparisonOperator.BEGINS_WITH, sortPrefix));
        final var request = QueryRequest.builder()
                .tableName(getTableName())
                .keyConditions(keyConditions)
                .build();
        return dynamoDbClient.query(request);
    }

    protected Condition getCondition(ComparisonOperator operator, String value) {
        final var attributeValueList = new ArrayList<AttributeValue>(1);
        attributeValueList.add(AttributeValue.builder().s(value).build());
        return Condition.builder()
                .attributeValueList(attributeValueList)
                .comparisonOperator(operator)
                .build();
    }

    protected String getSubValueOrDefault(String[] items, Integer index, String defaultValue) {
        if (items.length > index) {
            return items[index];
        }

        return defaultValue;
    }

    protected String getSecondValueOrDefault(GetItemResponse response, String attribute, String defaultValue) {
        var value = response.getValueForField(attribute, String.class);
        if (value.isPresent()) {
            var parts = value.get().split("#");
            if (parts.length > 1) {
                return parts[1];
            }
        }

        return defaultValue;
    }

    protected Date getDateOrDefault(GetItemResponse response, String attribute, Date defaultValue) {
        var value = response.getValueForField(attribute, String.class);
        if (value.isPresent()) {
            return new DateTime(value.get()).toDate();
        }

        return defaultValue;
    }

    protected Date getDateOrDefault(Map<String, AttributeValue> value, String attribute, Date defaultValue) {
        if (!value.containsKey(attribute)) {
            return defaultValue;
        }

        final var val = value.get(attribute);
        if (val == null) {
            return defaultValue;
        }

        return new DateTime(val.s()).toDate();
    }

    protected abstract String getKeyPrefix();

    private Map<String, AttributeValue> getEntityKeyMap(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id");
        }

        final var map = new HashMap<String, AttributeValue>();
        final String key = getKeyPrefix() + id;
        map.put(PK_ATTRIBUTE, AttributeValue.builder().s(key).build());
        map.put(SK_ATTRIBUTE, AttributeValue.builder().s(key).build());
        return map;
    }
}
