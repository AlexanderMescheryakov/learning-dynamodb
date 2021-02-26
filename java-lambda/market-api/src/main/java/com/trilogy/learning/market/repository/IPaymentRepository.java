package com.trilogy.learning.market.repository;

import com.trilogy.learning.market.model.Payment;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

import java.math.BigDecimal;
import java.util.List;

public interface IPaymentRepository {
    TransactWriteItem getAddPaymentTransactRequest(String customerId, BigDecimal amount);

    List<Payment> getByCustomerId(String customerId);
}
