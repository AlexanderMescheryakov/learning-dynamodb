package com.trilogy.learning.market.repository.dynamodb;

import java.math.BigDecimal;
import java.util.*;

import com.trilogy.learning.market.repository.ITableRepository;
import lombok.extern.jbosslog.JBossLog;
import org.joda.time.DateTime;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@JBossLog
public abstract class AbstractRepository<T> implements ITableRepository {
    public static final String SEP = "#";

    protected static final String PK_ATTRIBUTE= "pk";
    protected static final String SK_ATTRIBUTE = "sk";
    protected static final String GSI1 = "gsi1";
    protected static final String GSI1_PK_ATTRIBUTE= "gsi1pk";
    protected static final String GSI1_SK_ATTRIBUTE = "gsi1sk";
    protected static final String GSI2 = "GSI2";
    protected static final String GSI2_PK_ATTRIBUTE= "GSI2PK";
    protected static final String GSI2_SK_ATTRIBUTE = "GSI2SK";
    protected static final String DATA_ATTRIBUTE = "Data";

    protected final DynamoDbClient dynamoDbClient;

    protected AbstractRepository(DynamoDbClient dynamoClient) {
        dynamoDbClient = dynamoClient;
    }

    @Override
    public String getTableName() {
        return System.getenv("DYNAMODB_TABLE");
    }

    protected GetItemResponse getItem(String id) {
        final var keyMap = getEntityKeyMap(id);
        final var request = GetItemRequest.builder()
                .tableName(getTableName())
                .key(keyMap)
                .build();
        return dynamoDbClient.getItem(request);
    }

    protected BatchGetItemResponse getItemsByIds(Set<String> ids) {
        final var keys = new ArrayList<Map<String, AttributeValue>>();
        for (var id : ids) {
            final var keyMap = getEntityKeyMap(id);
            keys.add(keyMap);
        }

        var requestsForTable = KeysAndAttributes.builder().keys(keys).build();
        var requests = new HashMap<String, KeysAndAttributes>();
        requests.put(getTableName(), requestsForTable);
        final var request = BatchGetItemRequest.builder().requestItems(requests).build();
        return dynamoDbClient.batchGetItem(request);
    }

    protected PutItemResponse putNewItem(Map<String, AttributeValue> item) {
        final var request = PutItemRequest.builder()
                .tableName(getTableName())
                .conditionExpression("attribute_not_exists(" + PK_ATTRIBUTE + ")")
                .item(item)
                .returnValues(ReturnValue.NONE)
                .build();
        return dynamoDbClient.putItem(request);
    }

    protected PutItemResponse replaceItem(Map<String, AttributeValue> item) {
        final var request = PutItemRequest.builder()
                .tableName(getTableName())
                .item(item)
                .returnValues(ReturnValue.NONE)
                .build();
        return dynamoDbClient.putItem(request);
    }

    protected BatchWriteItemResponse putItems(List<Map<String, AttributeValue>> items) {
        var requests = new ArrayList<WriteRequest>();
        for (var item : items) {
            var putRequest = PutRequest.builder().item(item).build();
            var request = WriteRequest.builder().putRequest(putRequest).build();
            requests.add(request);
        }

        var requestItems = new HashMap<String, Collection<WriteRequest>>();
        requestItems.put(getTableName(), requests);
        final var request = BatchWriteItemRequest.builder()
                .requestItems(requestItems).build();
        return dynamoDbClient.batchWriteItem(request);
    }

    protected UpdateItemResponse updateItem(Map<String, AttributeValue> key, String updateExpression,
                                            Map<String, String> expressionNames,
                                            Map<String, AttributeValue> expressionValues) {
        final var request = UpdateItemRequest.builder()
                .tableName(getTableName())
                .key(key)
                .updateExpression(updateExpression)
                .expressionAttributeNames(expressionNames)
                .expressionAttributeValues(expressionValues)
                .returnValues(ReturnValue.ALL_NEW)
                .build();
        return dynamoDbClient.updateItem(request);
    }

    protected DeleteItemResponse deleteItem(Map<String, AttributeValue> key) {

        final var request = DeleteItemRequest.builder()
                .tableName(getTableName())
                .key(key)
                .returnValues(ReturnValue.ALL_OLD)
                .build();
        return dynamoDbClient.deleteItem(request);
    }

    protected Map<String, AttributeValue> getKeyAttributes(String pk, String sk) {
        var key = new HashMap<String, AttributeValue>();
        addStringAttribute(key, PK_ATTRIBUTE, pk);
        addStringAttribute(key, SK_ATTRIBUTE, sk);
        return key;
    }

    protected QueryResponse queryParent(String parentKeyAttribute, String parentKey,
                                           String sortAttribute, String sortPrefix) {
        final var keyConditions = new HashMap<String, Condition>();
        keyConditions.put(parentKeyAttribute, getCondition(ComparisonOperator.EQ, parentKey));
        keyConditions.put(sortAttribute, getCondition(ComparisonOperator.BEGINS_WITH, sortPrefix));
        return query(keyConditions);
    }

    protected QueryResponse queryPartition(String key) {
        final var keyConditions = new HashMap<String, Condition>();
        keyConditions.put(PK_ATTRIBUTE, getCondition(ComparisonOperator.EQ, key));
        return query(keyConditions);
    }

    protected QueryResponse queryGsi1Partition(String key) {
        final var keyConditions = new HashMap<String, Condition>();
        keyConditions.put(GSI1_PK_ATTRIBUTE, getCondition(ComparisonOperator.EQ, key));
        return queryGsi1(keyConditions);
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

    protected String getSecondValueOrDefault(Map<String, AttributeValue> value, String attribute, String defaultValue) {
        if (!value.containsKey(attribute)) {
            return defaultValue;
        }

        final var val = value.get(attribute);
        if (val == null || val.s() == null) {
            return defaultValue;
        }

        var parts = val.s().split("#");
        if (parts.length > 1) {
            return parts[1];
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

    protected BigDecimal getBigDecimalOrDefault(GetItemResponse response, String attribute, BigDecimal defaultValue) {
        var value = response.getValueForField(attribute, BigDecimal.class);
        if (value.isPresent()) {
            return value.get();
        }

        return defaultValue;
    }

    protected BigDecimal getBigDecimalOrDefault(Map<String, AttributeValue> value,
                                                String attribute, BigDecimal defaultValue) {
        if (!value.containsKey(attribute)) {
            return defaultValue;
        }

        final var val = value.get(attribute);
        if (val == null || val.n() == null) {
            return defaultValue;
        }

        return new BigDecimal(val.n());
    }

    protected Integer getIntegerOrDefault(Map<String, AttributeValue> value, String attribute, Integer defaultValue) {
        if (!value.containsKey(attribute)) {
            return defaultValue;
        }

        final var val = value.get(attribute);
        if (val == null || val.n() == null) {
            return defaultValue;
        }

        return Integer.parseInt(val.n());
    }

    protected String getStringOrDefault(Map<String, AttributeValue> value, String attribute, String defaultValue) {
        if (!value.containsKey(attribute)) {
            return defaultValue;
        }

        final var val = value.get(attribute);
        if (val == null || val.s() == null) {
            return defaultValue;
        }

        return val.s();
    }

    protected Date getDateOrDefault(Map<String, AttributeValue> value, String attribute, Date defaultValue) {
        if (!value.containsKey(attribute)) {
            return defaultValue;
        }

        final var val = value.get(attribute);
        if (val == null || val.s() == null) {
            return defaultValue;
        }

        return new DateTime(val.s()).toDate();
    }

    protected void addPkSkIdAttribute(Map<String, AttributeValue> item, String id) {
        addPrefixedStringAttribute(item, PK_ATTRIBUTE, id);
        addPrefixedStringAttribute(item, SK_ATTRIBUTE, id);
    }

    protected void addPkSkIdAttribute(Map<String, AttributeValue> item, String primaryId, String sortKey) {
        addPrefixedStringAttribute(item, PK_ATTRIBUTE, primaryId);
        addStringAttribute(item, SK_ATTRIBUTE, sortKey);
    }

    protected void addPkSkIdAttributeIntoParent(Map<String, AttributeValue> item, String childId, String parentKey) {
        addStringAttribute(item, PK_ATTRIBUTE, parentKey);
        addPrefixedStringAttribute(item, SK_ATTRIBUTE, childId);
    }

    protected void addPrefixedStringAttribute(Map<String, AttributeValue> item,
                                              String attributeName, String value) {
        addStringAttribute(item, attributeName, getKeyPrefix() + value);
    }

    protected void addStringAttribute(Map<String, AttributeValue> item, String attributeName, String value) {
        if (value == null) {
            return;
        }

        item.put(attributeName, AttributeValue.builder().s(value).build());
    }

    protected void addDateAttribute(Map<String, AttributeValue> item, String attributeName, Date value) {
        if (value == null) {
            return;
        }

        var isoDate = new DateTime(value).toString("yyyy-MM-dd'T'HH:mm:ss");
        item.put(attributeName, AttributeValue.builder().s(isoDate).build());
    }

    protected void addIntegerAttribute(Map<String, AttributeValue> item, String attributeName, Integer value) {
        if (value == null) {
            return;
        }

        item.put(attributeName, AttributeValue.builder().n(value.toString()).build());
    }

    protected void addBigDecimalAttribute(Map<String, AttributeValue> item, String attributeName, BigDecimal value) {
        if (value == null) {
            return;
        }

        item.put(attributeName, AttributeValue.builder().n(value.toPlainString()).build());
    }

    protected Map<String, AttributeValue> getItemKey(String id) {
        return getKeyAttributes(getKeyPrefix() + id, getKeyPrefix() + id);
    }

    protected abstract String getKeyPrefix();

    private QueryResponse query(Map<String, Condition> keyConditions) {
        final var request = QueryRequest.builder()
                .tableName(getTableName())
                .keyConditions(keyConditions)
                .build();
        return dynamoDbClient.query(request);
    }

    private QueryResponse queryGsi1(Map<String, Condition> keyConditions) {
        final var request = QueryRequest.builder()
                .tableName(getTableName())
                .keyConditions(keyConditions)
                .indexName(GSI1)
                .build();
        return dynamoDbClient.query(request);
    }

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
