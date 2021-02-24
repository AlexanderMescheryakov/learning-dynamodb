package com.trilogy.learning.market.repository.dynamodb;

import com.trilogy.learning.market.model.Address;
import com.trilogy.learning.market.model.Customer;
import com.trilogy.learning.market.repository.ICustomerRepository;
import com.trilogy.learning.market.requests.UpdateCustomerRequest;
import lombok.extern.jbosslog.JBossLog;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JBossLog
@Singleton
public class CustomerRepository extends AbstractRepository<Customer> implements ICustomerRepository {
    static final String KEY_PREFIX = "CUST#";
    static final String ADDR_PREFIX = "CUST_ADDR#";
    private static final String NAME_ATTRIBUTE = GSI1_SK_ATTRIBUTE;
    private static final String COUNTRY_CITY_ATTRIBUTE = GSI1_PK_ATTRIBUTE;
    private static final String STREET_ADDRESS_ATTRIBUTE = "StreetAddress";
    private static final String ORDER_COUNT_ATTRIBUTE = DATA_ATTRIBUTE;

    @Inject
    public CustomerRepository(DynamoDbClient dynamoDbClient) {
        super(dynamoDbClient);
    }

    @Override
    public Customer getById(String id) {
        final var response = getItem(id);
        return getCustomerFromItem(response.item());
    }

    @Override
    public Customer getByOrderId(String orderId) {
        final var response = queryGsi1Partition(OrderRepository.KEY_PREFIX + orderId);
        log.info(response);
        final var customers = getCustomersFromItems(response.items());
        if (customers.size() > 0) {
            return customers.get(0);
        }

        return null;
    }

    @Override
    public void addCustomer(Customer customer) {
        var item = getItemFromCustomer(customer);
        putNewItem(item);
    }

    @Override
    public Customer updateCustomer(UpdateCustomerRequest request) {
        final var updateValues = new HashMap<String, AttributeValue>();
        String updateExpression = "";
        if (request.getName() != null) {
            updateExpression += "SET " + NAME_ATTRIBUTE + "=:name";
            updateValues.put(":name", AttributeValue.builder().s(KEY_PREFIX + request.getName()).build());
        }

        if (request.getAddress() != null) {
            if (updateExpression.length() > 0) {
                updateExpression += ", ";
            }

            updateExpression += "SET " + STREET_ADDRESS_ATTRIBUTE + "=:st, "
                + COUNTRY_CITY_ATTRIBUTE + "=:ct";
            final var countryCity = ADDR_PREFIX + request.getAddress().getCountry()
                    + SEP + request.getAddress().getCity();
            updateValues.put(":st", AttributeValue.builder().s(request.getAddress().getStreetAddress()).build());
            updateValues.put(":ct", AttributeValue.builder().s(countryCity).build());
        }

        if (updateValues.size() > 0) {
            final var response = updateItem(getCustomerKey(request.getId()),
                    updateExpression, null, updateValues);
            return getCustomerFromItem(response.attributes());
        }

        return null;
    }

    @Override
    public void deleteCustomer(String id) {
        deleteItem(getItemKey(id));
    }

    private Map<String, AttributeValue> getItemFromCustomer(Customer customer) {
        var item = new HashMap<String, AttributeValue>();
        addPkSkIdAttribute(item, customer.getEmail());
        addPrefixedStringAttribute(item, NAME_ATTRIBUTE, customer.getName());
        addAddressValue(item, customer.getAddress());
        return item;
    }

    private Customer getCustomerFromItem(Map<String, AttributeValue> item) {
        if (isCustomerEntity(item)) {
            return Customer.builder()
                    .email(getSecondValueOrDefault(item, PK_ATTRIBUTE, ""))
                    .name(getSecondValueOrDefault(item, NAME_ATTRIBUTE, ""))
                    .address(getAddressFromItem(item))
                    .orderCount(getIntegerOrDefault(item, ORDER_COUNT_ATTRIBUTE, 0))
                    .build();
        }

        return Customer.builder()
                .email(getSecondValueOrDefault(item, PK_ATTRIBUTE, ""))
                .build();
    }

    private boolean isCustomerEntity(Map<String, AttributeValue> item) {
        return getStringOrDefault(item, SK_ATTRIBUTE, "").startsWith(KEY_PREFIX);
    }

    private List<Customer> getCustomersFromItems(List<Map<String, AttributeValue>> items) {
        final var customers = new ArrayList<Customer>();
        for (var item : items) {
            final var customer = getCustomerFromItem(item);
            if (customer != null) {
                customers.add(customer);
            }
        }

        return customers;
    }

    private Address getAddressFromItem(Map<String, AttributeValue> item) {
        var combinedAddress = getStringOrDefault(item, COUNTRY_CITY_ATTRIBUTE, "");
        var parts = combinedAddress.split(SEP);
        var streetAddress = getStringOrDefault(item, STREET_ADDRESS_ATTRIBUTE, "");
        return Address.builder()
                .country(getSubValueOrDefault(parts, 1, ""))
                .city(getSubValueOrDefault(parts, 2, ""))
                .streetAddress(streetAddress)
                .build();
    }

    private void addAddressValue(Map<String, AttributeValue> item, Address address) {
        if (address == null) {
            return;
        }

        addStringAttribute(item, STREET_ADDRESS_ATTRIBUTE, address.getStreetAddress());
        addStringAttribute(item, COUNTRY_CITY_ATTRIBUTE,
                ADDR_PREFIX + address.getCountry() + SEP + address.getCity());
    }

    private Map<String, AttributeValue> getCustomerKey(String id) {
        return getKeyAttributes(KEY_PREFIX + id, KEY_PREFIX + id);
    }

    @Override
    protected String getKeyPrefix() {
        return KEY_PREFIX;
    }
}