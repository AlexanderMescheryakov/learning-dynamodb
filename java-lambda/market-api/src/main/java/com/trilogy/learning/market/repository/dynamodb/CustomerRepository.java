package com.trilogy.learning.market.repository.dynamodb;

import com.trilogy.learning.market.model.Address;
import com.trilogy.learning.market.model.Customer;
import com.trilogy.learning.market.repository.ICustomerRepository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

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
        return getCustomerFromResponse(response, id);
    }

    @Override
    public void addCustomer(Customer customer) {
        var item = getItemFromCustomer(customer);
        putNewItem(item);
    }

    @Override
    public void updateCustomer(Customer customer) {
        var item = getItemFromCustomer(customer);
        replaceItem(item);
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

    private Customer getCustomerFromResponse(GetItemResponse response, String email) {
        return Customer.builder()
                .email(email)
                .name(getSecondValueOrDefault(response, NAME_ATTRIBUTE, ""))
                .address(getAddressFromResponse(response))
                .orderCount(response.getValueForField(ORDER_COUNT_ATTRIBUTE, Integer.class).orElse(0))
                .build();
    }

    private Address getAddressFromResponse(GetItemResponse response) {
        var combinedAddress = response.getValueForField(COUNTRY_CITY_ATTRIBUTE, String.class).orElse("");
        var parts = combinedAddress.split("#");
        var streetAddress = response.getValueForField(STREET_ADDRESS_ATTRIBUTE, String.class).orElse(null);
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
                ADDR_PREFIX + address.getCountry() + "#" + address.getCity());
    }

    @Override
    protected String getKeyPrefix() {
        return KEY_PREFIX;
    }
}