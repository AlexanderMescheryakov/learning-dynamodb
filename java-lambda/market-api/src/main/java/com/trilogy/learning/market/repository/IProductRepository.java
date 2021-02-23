package com.trilogy.learning.market.repository;

import com.trilogy.learning.market.model.Product;

import java.util.Map;
import java.util.Set;

public interface IProductRepository {
    Product getById(String id);

    Map<String, Product> getByIds(Set<String> ids);

    void addProduct(Product product);

    void updateProduct(Product product);

    void deleteProduct(String id);
}
