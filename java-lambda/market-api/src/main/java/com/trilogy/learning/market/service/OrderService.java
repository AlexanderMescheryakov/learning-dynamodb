package com.trilogy.learning.market.service;

import com.amirkhawaja.Ksuid;
import com.trilogy.learning.market.model.Order;
import com.trilogy.learning.market.model.OrderedProduct;
import com.trilogy.learning.market.model.Product;
import com.trilogy.learning.market.repository.IOrderRepository;
import com.trilogy.learning.market.repository.IProductRepository;
import com.trilogy.learning.market.requests.NewOrder;
import com.trilogy.learning.market.requests.NewOrderedProduct;
import lombok.AllArgsConstructor;

import javax.inject.Singleton;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Singleton
@AllArgsConstructor
public class OrderService implements IOrderService {
    private IOrderRepository orderRepository;
    private IProductRepository productRepository;

    public void addOrder(NewOrder orderRequest) throws IOException {
        final var uid = new Ksuid();
        final var orderId = uid.generate();
        final var productIds = orderRequest.getProducts().keySet();
        final var products = productRepository.getByIds(productIds);
        final var orderedProducts = getOrderedProducts(products, orderRequest.getProducts(), orderId);
        final var total = OrderedProduct.getTotal(orderedProducts);
        final var order = Order.builder()
                .id(orderId)
                .createdAt(new Date())
                .customerEmail(orderRequest.getCustomeEmail())
                .products(orderedProducts)
                .status(Order.Status.OPEN)
                .total(total)
                .build();
        orderRepository.addOrder(order);
    }

    private List<OrderedProduct> getOrderedProducts(Map<String, Product> products,
                                                    Map<String, NewOrderedProduct> orderedProductRequests,
                                                    String orderId) {
        final var orderedProducts = new ArrayList<OrderedProduct>();
        for (var productEntry : products.entrySet()) {
            var product = productEntry.getValue();
            var orderedProductRequest = orderedProductRequests.get(product.getId());
            var orderedProduct = OrderedProduct.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .orderId(orderId)
                    .price(product.getPrice())
                    .quantity(orderedProductRequest.getQuantity())
                    .build();
            orderedProducts.add(orderedProduct);
        }

        return orderedProducts;
    }
}
