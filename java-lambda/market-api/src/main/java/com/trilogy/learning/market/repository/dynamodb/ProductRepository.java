package com.trilogy.learning.market.repository.dynamodb;

import com.trilogy.learning.market.model.Product;
import com.trilogy.learning.market.repository.IProductRepository;
import com.trilogy.learning.market.repository.dynamodb.entity.Metadata;
import lombok.extern.jbosslog.JBossLog;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.*;

import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.*;

@JBossLog
@Singleton
public class ProductRepository extends AbstractRepository<Product> implements IProductRepository {
    static final String KEY_PREFIX = "PROD#";
    static final String CATEGORY_PREFIX = "CAT#";
    static final String OUT_OF_STOCK_MARKER = "OUT_OF_STOCK";

    private static final String CATEGORY_ATTRIBUTE = GSI1_PK_ATTRIBUTE;
    private static final String NAME_ATTRIBUTE = GSI1_SK_ATTRIBUTE;
    private static final String PRICE_ATTRIBUTE = DATA_ATTRIBUTE;

    private static final PrefixAttributeConverter idConverter = new PrefixAttributeConverter(KEY_PREFIX);
    private static final PrefixAttributeConverter categoryConverter = new PrefixAttributeConverter(CATEGORY_PREFIX);
    private static final StaticTableSchema<Product> TABLE_SCHEMA =
            StaticTableSchema.builder(Product.class)
                    .newItemSupplier(Product::new)
                    .extend(Metadata.getSchema())
                    .addAttribute(String.class, a -> a.name(PK_ATTRIBUTE)
                            .getter(Product::getId)
                            .setter(Product::setId)
                            .tags(primaryPartitionKey())
                            .attributeConverter(idConverter))
                    .addAttribute(String.class, a -> a.name(SK_ATTRIBUTE)
                            .getter(Product::getId)
                            .setter(Product::setId)
                            .tags(primarySortKey())
                            .attributeConverter(idConverter))
                    .addAttribute(String.class, a -> a.name(GSI1_PK_ATTRIBUTE)
                            .getter(Product::getCategory)
                            .setter(Product::setCategory)
                            .attributeConverter(categoryConverter)
                            .tags(secondaryPartitionKey(GSI1), updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
                    .addAttribute(String.class, a -> a.name(GSI1_SK_ATTRIBUTE)
                            .getter(Product::getName)
                            .setter(Product::setName)
                            .tags(secondarySortKey(GSI1), updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
                    .addAttribute(BigDecimal.class, a -> a.name(DATA_ATTRIBUTE)
                            .getter(Product::getPrice)
                            .setter(Product::setPrice)
                            .tags(updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
                    .addAttribute(String.class, a -> a.name(GSI2_PK_ATTRIBUTE)
                            .getter(Product::getOutOfStockMarker)
                            .setter(Product::setOutOfStockMarker)
                            .tags(secondaryPartitionKey(GSI2)))
                    .addAttribute(String.class, a -> a.name(GSI2_SK_ATTRIBUTE)
                            .getter(Product::getOutOfStockName)
                            .setter(Product::setOutOfStockName)
                            .tags(secondarySortKey(GSI2)))
                    .build();

    private final DynamoDbTable<Product> productTable;
    private final DynamoDbIndex<Product> productsByCategoryIndex;
    private final DynamoDbIndex<Product> outOfStockIndex;

    @Inject
    public ProductRepository(DynamoDbClient dynamoDbClient) {
        super(dynamoDbClient);
        final var enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
        productTable = enhancedClient.table(getTableName(), TABLE_SCHEMA);
        productsByCategoryIndex = productTable.index(GSI1);
        outOfStockIndex = productTable.index(GSI2);
    }

    @Override
    public Product getById(String id) {
        final var key = getPrimaryKey(id);
        return productTable.getItem(key);
    }

    @Override
    public List<Product> getByCategory(String category) {
        final var pk = getCategoryKey(category);
        final var products = new ArrayList<Product>();
        productsByCategoryIndex.query(keyEqualTo(k -> k.partitionValue(pk))).stream()
                .forEach(p -> products.addAll(p.items()));
        return products;
    }

    @Override
    public List<Product> getIfOutOfStock() {
        final var pk = getOutOfStockKey();
        final var products = new ArrayList<Product>();
        outOfStockIndex.query(keyEqualTo(k -> k.partitionValue(pk))).stream()
                .forEach(p -> products.addAll(p.items()));
        return products;
    }

    @Override
    public Map<String, Product> getByIds(Set<String> ids) {
        final var products = new HashMap<String, Product>();
        final var response = getItemsByIds(ids);
        if (!response.responses().containsKey(getTableName())) {
            return products;
        }

        final var items = response.responses().get(getTableName());
        for (var item : items) {
            final var product = getProductFromItem(item);
            products.put(product.getId(), product);
        }

        return products;
    }

    @Override
    public void addProduct(Product product) {
        productTable.putItem(product);
    }

    @Override
    public Product updateProduct(Product product) {
        return productTable.updateItem(product);
    }

    @Override
    public Product deleteProduct(String id) {
        final var key = getPrimaryKey(id);
        return productTable.deleteItem(key);
    }

    private Key getPrimaryKey(String id) {
        final var pk = getPartitionKey(id);
        final var sk = getSortKey(id);
        return Key.builder().partitionValue(pk).sortValue(sk).build();
    }

    private String getPartitionKey(String id) {
        return KEY_PREFIX + id;
    }

    private String getSortKey(String id) {
        return KEY_PREFIX + id;
    }

    private String getCategoryKey(String category) {
        return CATEGORY_PREFIX + category;
    }

    private String getOutOfStockKey() {
        return OUT_OF_STOCK_MARKER;
    }

    private Product getProductFromItem(Map<String, AttributeValue> item) {
        return TABLE_SCHEMA.mapToItem(item);
    }

    @Override
    protected String getKeyPrefix() {
        return KEY_PREFIX;
    }
}
