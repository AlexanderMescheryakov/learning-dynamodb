package com.trilogy.learning.market.repository;

import com.trilogy.learning.market.model.Customer;

public interface ICustomerRepository {
    Customer getById(String email);

    void addCustomer(Customer customer);

    void updateCustomer(Customer customer);

    void deleteCustomer(String id);
}
