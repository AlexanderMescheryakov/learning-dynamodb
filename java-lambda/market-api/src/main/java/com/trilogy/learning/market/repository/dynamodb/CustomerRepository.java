package com.trilogy.learning.market.repository.dynamodb;

import com.trilogy.learning.market.model.Address;
import com.trilogy.learning.market.model.Customer;
import com.trilogy.learning.market.repository.ICustomerRepository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CustomerRepository extends AbstractRepository<Customer> implements ICustomerRepository {
    public static final String KEY_PREFIX = "CUST#";

    @Inject
    public CustomerRepository(DynamoDbClient dynamoDbClient) {
        super(dynamoDbClient);
    }

    @Override
    public Customer getByEmail(String email) {
        final var response = getItem(email);
        return getCustomerFromResponse(response, email);
    }

    private Customer getCustomerFromResponse(GetItemResponse response, String email) {
        return Customer.builder()
                .email(email)
                .name(getSecondValueOrDefault(response, GSI1_SK_ATTRIBUTE, ""))
                .address(getAddressFromResponse(response))
                .build();
    }

    private Address getAddressFromResponse(GetItemResponse response) {
        var combinedAddress = response.getValueForField(GSI1_PK_ATTRIBUTE, String.class).orElse("");
        var parts = combinedAddress.split("#");
        return Address.builder()
                .country(getSubValueOrDefault(parts, 1, ""))
                .city(getSubValueOrDefault(parts, 2, ""))
                .build();
    }

    @Override
    protected String getKeyPrefix() {
        return KEY_PREFIX;
    }
}