package com.trilogy.learning.market.repository;

import com.trilogy.learning.market.model.Product;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IProductRepository {
    Product getById(String id);

    Map<String, Product> getByIds(Set<String> ids);

    List<Product> getByCategory(String category);

    void addProduct(Product product);

    Product updateProduct(Product product);

    Product deleteProduct(String id);
}
