package com.trilogy.learning.market.repository;

import com.trilogy.learning.market.model.Customer;

public interface ICustomerRepository {
    Customer getByEmail(String email);
}
