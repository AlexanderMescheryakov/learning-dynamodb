package com.trilogy.learning.market.repository.dynamodb;

import com.trilogy.learning.market.model.Order;
import com.trilogy.learning.market.model.OrderedProduct;
import com.trilogy.learning.market.repository.IOrderRepository;
import com.trilogy.learning.market.repository.dynamodb.entity.EntityType;
import com.trilogy.learning.market.repository.dynamodb.entity.Metadata;
import com.trilogy.learning.market.requests.UpdateOrderRequest;
import lombok.extern.jbosslog.JBossLog;
import org.joda.time.DateTime;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.rmi.Remote;
import java.util.*;

import static java.lang.String.format;

@JBossLog
@Singleton
public class OrderRepository extends AbstractRepository<Order> implements IOrderRepository {
    static final String KEY_PREFIX = "ORDER#";
    // Order Entity
    private static final String CUSTOMER_EMAIL_ATTRIBUTE = "CustomerEmail";
    private static final String DATE_DELIVERED_ATTRIBUTE = "DateDelivered";
    private static final String STATUS_ATTRIBUTE = GSI1_PK_ATTRIBUTE;
    private static final String DATE_CREATED_ATTRIBUTE = GSI1_SK_ATTRIBUTE;
    private static final String TOTAL_ATTRIBUTE = DATA_ATTRIBUTE;
    // Orders in the Customer -> Order relation
    private static final String CUSTOMERS_ORDER_STATUS_AND_ID_ATTRIBUTE = SK_ATTRIBUTE;
    private static final String CUSTOMERS_ORDER_ID_ATTRIBUTE = GSI1_PK_ATTRIBUTE;
    private static final String CUSTOMERS_ORDER_DATE_CREATED_ATTRIBUTE = GSI1_SK_ATTRIBUTE;
    private static final String CUSTOMERS_ORDER_DATE_DELIVERED_ATTRIBUTE = DATE_DELIVERED_ATTRIBUTE;
    private static final String CUSTOMERS_ORDER_TOTAL_ATTRIBUTE = DATA_ATTRIBUTE;
    // OrderedProduct Entity
    private static final String ORDERED_PRODUCT_ID_ATTRIBUTE = SK_ATTRIBUTE;
    private static final String ORDERED_PRODUCT_ID_PK_ATTRIBUTE = GSI1_PK_ATTRIBUTE;
    private static final String ORDERED_PRODUCT_ORDER_ID_ATTRIBUTE = GSI1_SK_ATTRIBUTE;
    private static final String ORDERED_PRODUCT_NAME_ATTRIBUTE = "Name";
    private static final String ORDERED_PRODUCT_PRICE_ATTRIBUTE = "Price";
    private static final String ORDERED_PRODUCT_QUANTITY_ATTRIBUTE = DATA_ATTRIBUTE;

    @Inject
    public OrderRepository(DynamoDbClient dynamoDbClient) {
        super(dynamoDbClient);
    }

    @Override
    public Order getById(String id) {
        final var response = getItem(id);
        return getOrderFromItem(response.item());
    }

    @Override
    public Order getByIdDetails(String id) {
        final var response = queryPartition(KEY_PREFIX + id);
        return getOrderDetailsFromResponse(response);
    }

    @Override
    public List<Order> getByCustomerEmail(String email) {
        final var response = queryParent(PK_ATTRIBUTE, CustomerRepository.KEY_PREFIX + email,
                SK_ATTRIBUTE, KEY_PREFIX);
        return getCustomerOrdersFromResponse(response, email);
    }

    @Override
    public List<Order> getByProductId(String productId) {
        final var response = queryGsi1Partition(ProductRepository.KEY_PREFIX + productId);
        return getOrdersFromResponse(response);
    }

    @Override
    public List<Order> getByStatus(Order.Status status, String month) {
        var key = KEY_PREFIX + status.toString();
        if (month != null && month.length() > 0) {
            key += SEP + month;
        }

        final var response = queryGsi1Partition(key);
        return getOrdersFromResponse(response);
    }

    @Override
    public void addOrder(Order order) {
        final var items = getItemsFromOrderDetails(order);
        putItems(items);
    }

    @Override
    public Order updateOrder(UpdateOrderRequest order) {
        final var updateValues = new HashMap<String, AttributeValue>();
        String updateExpression = "";
        if (order.getStatus() != null) {
            updateExpression += "SET " + STATUS_ATTRIBUTE + "=:status";
            final var now = new Date();
            final var value = getStatusDateValue(order.getStatus(), now);
            if (order.getStatus() == Order.Status.DELIVERED) {
                var date = new DateTime(now);
                updateValues.put(":dateDelivered", AttributeValue.builder()
                        .s(date.toString("yyyy-MM-dd'T'HH:mm:ss")).build());
                updateExpression += ", " + DATE_DELIVERED_ATTRIBUTE + "=:dateDelivered";
            }

            updateValues.put(":status", AttributeValue.builder().s(value).build());
        }

        if (updateValues.size() > 0) {
            final var response = updateItem(getOrderKey(order.getId()), updateExpression, null, updateValues);
            return getOrderFromItem(response.attributes());
        }

        return null;
    }

    private String getStatusDateValue(Order.Status status, Date date) {
        var value = KEY_PREFIX + status.toString();
        if (status == Order.Status.DELIVERED) {
            var dateFormatter = new DateTime(date);
            value += SEP + dateFormatter.toString("yyyy_MM");
        }

        return value;
    }

    @Override
    public Order deleteOrder(String id) {
        var response = deleteItem(getOrderKey(id));
        return getOrderFromItem(response.attributes());
    }

    private Map<String, AttributeValue> getOrderKey(String id) {
        return getKeyAttributes(KEY_PREFIX + id, KEY_PREFIX + id);
    }

    private List<Order> getCustomerOrdersFromResponse(QueryResponse response, String email) {
        final var orders = new ArrayList<Order>();
        if (!response.hasItems()) {
            return orders;
        }

        final var items = response.items();
        System.out.println("getOrdersFromResponse(): response.items()=" + items.toString());
        for (var item : items) {
            final var order = getCustomerOrderFromItem(item, email);
            if (order != null) {
                orders.add(order);
            }
        }

        return orders;
    }

    private Order getOrderDetailsFromResponse(QueryResponse response) {
        if (!response.hasItems()) {
            return null;
        }

        Order order = null;
        final var products = new ArrayList<OrderedProduct>();
        final var items = response.items();
        for (var item : items) {
            if (item.get(SK_ATTRIBUTE).s().startsWith(KEY_PREFIX)) {
                order = getOrderFromItem(item);
            } else if (item.get(SK_ATTRIBUTE).s().startsWith(ProductRepository.KEY_PREFIX)) {
                final var product = getOrderedProductFromAttributeValue(item);
                if (product != null) {
                    products.add(product);
                }
            }
        }

        if (order == null) {
            return null;
        }

        order.setProducts(products);
        return order;
    }

    private List<Order> getOrdersFromResponse(QueryResponse response) {
        final var orders = new ArrayList<Order>();
        if (!response.hasItems()) {
            return orders;
        }

        final var items = response.items();
        for (var item : items) {
            if (item.get(PK_ATTRIBUTE).s().startsWith(KEY_PREFIX)) {
                final var order = getOrderFromItem(item);
                orders.add(order);
            }
        }

        return orders;
    }

    @Override
    public Order getOrderFromItem(Map<String, AttributeValue> item) {
        log.info(item);
        if (isOrderEntity(item)) {
            return Order.builder()
                    .id(getSecondValueOrDefault(item, PK_ATTRIBUTE, null))
                    .status(Order.Status.valueOf(getSecondValueOrDefault(item, STATUS_ATTRIBUTE, null)))
                    .createdAt(getDateOrDefault(item, DATE_CREATED_ATTRIBUTE, null))
                    .deliveredAt(getDateOrDefault(item, DATE_DELIVERED_ATTRIBUTE, null))
                    .customerEmail(getStringOrDefault(item, CUSTOMER_EMAIL_ATTRIBUTE, ""))
                    .total(getBigDecimalOrDefault(item, TOTAL_ATTRIBUTE, null))
                    .build();
        }

        return Order.builder()
                .id(getSecondValueOrDefault(item, PK_ATTRIBUTE, null))
                .build();
    }

    @Override
    public List<TransactWriteItem> getChangeStatusTransactRequest(String customerId, String orderId,
                                                                  Order oldOrder, Order.Status newStatus) {
        final var requests = new ArrayList<TransactWriteItem>();
        final var updateRequest = Update.builder()
                .tableName(getTableName())
                .key(getOrderKey(orderId))
                .conditionExpression(format("%s = :old", STATUS_ATTRIBUTE))
                .updateExpression(format("SET %s = :new", STATUS_ATTRIBUTE))
                .expressionAttributeValues(Map.of(
                        ":new" , getStatusAttributeValue(newStatus),
                        ":old" , getStatusAttributeValue(Order.Status.OPEN)))
                .build();
        requests.add(TransactWriteItem.builder().update(updateRequest).build());
        final var deleteRequest = Delete.builder()
                .tableName(getTableName())
                .key(getCustomerOrderKey(customerId, orderId, oldOrder.getStatus()))
                .conditionExpression(format("attribute_exists(%s)", PK_ATTRIBUTE))
                .build();
        requests.add(TransactWriteItem.builder().delete(deleteRequest).build());
        final var newCustomerOrder = oldOrder.toBuilder().status(newStatus).build();
        final var newCustomerOrderItem = getCustomerOrderItemFromOrder(newCustomerOrder);
        final var putRequest = Put.builder()
                .tableName(getTableName())
                .conditionExpression(format("attribute_not_exists(%s)", PK_ATTRIBUTE))
                .item(newCustomerOrderItem)
                .build();
        requests.add(TransactWriteItem.builder().put(putRequest).build());
        return requests;
    }

    private Map<String, AttributeValue> getCustomerOrderKey(String customerId, String orderId, Order.Status status) {
        final var key = new HashMap<String, AttributeValue>();
        addPkSkIdAttributeIntoParent(key, getCustomerOrderStatusValue(status, orderId),
                CustomerRepository.getPartitonKey(customerId));
        return key;
    }

    private AttributeValue getStatusAttributeValue(Order.Status status) {
        return AttributeValue.builder().s(getStatusDateValue(status, new Date())).build();
    }

    private String getCustomerOrderStatusValue(Order.Status status, String orderId) {
        return format("%s#%s", status.toString(), orderId);
    }

    private boolean isOrderEntity(Map<String, AttributeValue> item) {
        return getStringOrDefault(item, SK_ATTRIBUTE, "").startsWith(KEY_PREFIX);
    }

    private Order getCustomerOrderFromItem(Map<String, AttributeValue> item, String email) {
        final var orderStatusAndId = item.get(CUSTOMERS_ORDER_STATUS_AND_ID_ATTRIBUTE).s();
        var parts = orderStatusAndId.split(SEP);
        if (parts.length < 3) {
            return null;
        }

        return Order.builder()
                .status(Order.Status.valueOf(parts[1]))
                .id(parts[2])
                .createdAt(getDateOrDefault(item, CUSTOMERS_ORDER_DATE_CREATED_ATTRIBUTE, null))
                .deliveredAt(getDateOrDefault(item, CUSTOMERS_ORDER_DATE_DELIVERED_ATTRIBUTE, null))
                .total(getBigDecimalOrDefault(item, CUSTOMERS_ORDER_TOTAL_ATTRIBUTE, null))
                .customerEmail(email)
                .build();
    }

    private OrderedProduct getOrderedProductFromAttributeValue(Map<String, AttributeValue> value) {
        log.info("getOrderedProductFromAttributeValue(): value=" + value.toString());
        return OrderedProduct.builder()
                .id(getSecondValueOrDefault(value, ORDERED_PRODUCT_ID_ATTRIBUTE, null))
                .orderId(getSecondValueOrDefault(value, ORDERED_PRODUCT_ORDER_ID_ATTRIBUTE, null))
                .name(getStringOrDefault(value, ORDERED_PRODUCT_NAME_ATTRIBUTE, ""))
                .price(getBigDecimalOrDefault(value, ORDERED_PRODUCT_PRICE_ATTRIBUTE, null))
                .quantity(getIntegerOrDefault(value, ORDERED_PRODUCT_QUANTITY_ATTRIBUTE, null))
                .build();
    }

    private Map<String, AttributeValue> getItemFromOrderedProduct(OrderedProduct product, String orderId) {
        var item = new HashMap<String, AttributeValue>();
        addPkSkIdAttribute(item, orderId, ProductRepository.KEY_PREFIX + product.getId());
        addPrefixedStringAttribute(item, ORDERED_PRODUCT_ORDER_ID_ATTRIBUTE, orderId);
        addStringAttribute(item, ORDERED_PRODUCT_ID_PK_ATTRIBUTE, ProductRepository.KEY_PREFIX + product.getId());
        addIntegerAttribute(item, ORDERED_PRODUCT_QUANTITY_ATTRIBUTE, product.getQuantity());
        addBigDecimalAttribute(item, ORDERED_PRODUCT_PRICE_ATTRIBUTE, product.getPrice());
        addStringAttribute(item, ORDERED_PRODUCT_NAME_ATTRIBUTE, product.getName());
        return item;
    }

    private Map<String, AttributeValue> getItemFromOrder(Order order) {
        var item = new HashMap<String, AttributeValue>();
        addPkSkIdAttribute(item, order.getId());
        addStringAttribute(item, CUSTOMER_EMAIL_ATTRIBUTE, order.getCustomerEmail());
        addDateAttribute(item, DATE_CREATED_ATTRIBUTE, order.getCreatedAt());
        addDateAttribute(item, DATE_DELIVERED_ATTRIBUTE, order.getDeliveredAt());
        addStringAttribute(item, STATUS_ATTRIBUTE, getStatusDateValue(order.getStatus(), new Date()));
        addBigDecimalAttribute(item, TOTAL_ATTRIBUTE, order.getTotal());
        addStringAttribute(item, Metadata.getEntityTypeAttributeName(), EntityType.Order.toString());
        return item;
    }

    private List<Map<String, AttributeValue>> getItemsFromOrderDetails(Order order) {
        var orderItem = getItemFromOrder(order);
        var items = new ArrayList<Map<String, AttributeValue>>();
        items.add(orderItem);
        var customerOrderRelationItem = getCustomerOrderItemFromOrder(order);
        items.add(customerOrderRelationItem);
        for (var product : order.getProducts()) {
            var orderedProductItem = getItemFromOrderedProduct(product, order.getId());
            items.add(orderedProductItem);
        }

        return items;
    }

    private Map<String, AttributeValue> getCustomerOrderItemFromOrder(Order order) {
        var item = new HashMap<String, AttributeValue>();
        addPkSkIdAttributeIntoParent(item, order.getStatus().toString() + SEP + order.getId(),
                CustomerRepository.getPartitonKey(order.getCustomerEmail()));
        addDateAttribute(item, CUSTOMERS_ORDER_DATE_CREATED_ATTRIBUTE, order.getCreatedAt());
        addDateAttribute(item, CUSTOMERS_ORDER_DATE_DELIVERED_ATTRIBUTE, order.getDeliveredAt());
        addBigDecimalAttribute(item, CUSTOMERS_ORDER_TOTAL_ATTRIBUTE, order.getTotal());
        addPrefixedStringAttribute(item, CUSTOMERS_ORDER_ID_ATTRIBUTE, order.getId());
        addStringAttribute(item, Metadata.getEntityTypeAttributeName(), EntityType.CustomerOrder.toString());
        return item;
    }

    @Override
    protected String getKeyPrefix() {
        return KEY_PREFIX;
    }
}
