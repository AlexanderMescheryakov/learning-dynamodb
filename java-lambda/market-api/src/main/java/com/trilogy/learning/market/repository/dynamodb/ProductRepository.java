package com.trilogy.learning.market.repository.dynamodb;

import com.trilogy.learning.market.model.Product;
import com.trilogy.learning.market.repository.IProductRepository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
public class ProductRepository extends AbstractRepository<Product> implements IProductRepository {
    static final String KEY_PREFIX = "PROD#";
    static final String CATEGORY_PREFIX = "CAT#";

    private static final String CATEGORY_ATTRIBUTE = GSI1_PK_ATTRIBUTE;
    private static final String NAME_ATTRIBUTE = GSI1_SK_ATTRIBUTE;
    private static final String PRICE_ATTRIBUTE = DATA_ATTRIBUTE;

    @Inject
    public ProductRepository(DynamoDbClient dynamoDbClient) {
        super(dynamoDbClient);
    }

    @Override
    public Product getById(String id) {
        final var response = getItem(id);
        return getProductFromResponse(response, id);
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
        var item = getItemFromProduct(product);
        putNewItem(item);
    }

    @Override
    public void updateProduct(Product product) {
        var item = getItemFromProduct(product);
        replaceItem(item);
    }

    @Override
    public void deleteProduct(String id) {
        deleteItem(getItemKey(id));
    }

    private Product getProductFromResponse(GetItemResponse response, String id) {
        return Product.builder()
                .id(id)
                .name(response.getValueForField(NAME_ATTRIBUTE, String.class).orElse(""))
                .price(new BigDecimal(response.item().get(PRICE_ATTRIBUTE).n()))
                .category(getSecondValueOrDefault(response, CATEGORY_ATTRIBUTE, ""))
                .build();
    }

    private Product getProductFromItem(Map<String, AttributeValue> item) {
        return Product.builder()
                .id(getSecondValueOrDefault(item, PK_ATTRIBUTE, null))
                .name(getStringOrDefault(item, NAME_ATTRIBUTE, null))
                .price(getBigDecimalOrDefault(item, PRICE_ATTRIBUTE, null))
                .category(getSecondValueOrDefault(item, CATEGORY_ATTRIBUTE, ""))
                .build();
    }

    private Map<String, AttributeValue> getItemFromProduct(Product product) {
        var item = new HashMap<String, AttributeValue>();
        addPkSkIdAttribute(item, product.getId());
        addStringAttribute(item, NAME_ATTRIBUTE, product.getName());
        addStringAttribute(item, CATEGORY_ATTRIBUTE, CATEGORY_PREFIX + product.getCategory());
        return item;
    }

    @Override
    protected String getKeyPrefix() {
        return KEY_PREFIX;
    }
}
