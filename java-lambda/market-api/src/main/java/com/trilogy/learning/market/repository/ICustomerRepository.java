package com.trilogy.learning.market.repository;

import com.trilogy.learning.market.model.Customer;
import com.trilogy.learning.market.requests.UpdateCustomerRequest;

public interface ICustomerRepository {
    Customer getById(String email);

    Customer getByOrderId(String orderId);

    void addCustomer(Customer customer);

    Customer updateCustomer(UpdateCustomerRequest customer);

    void deleteCustomer(String id);
}
