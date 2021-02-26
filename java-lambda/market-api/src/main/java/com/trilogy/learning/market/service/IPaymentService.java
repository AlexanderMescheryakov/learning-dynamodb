package com.trilogy.learning.market.service;

import com.trilogy.learning.market.model.Payment;

import java.math.BigDecimal;

public interface IPaymentService {
    Payment.Status handleOrderPayment(String customerId, String orderId, BigDecimal amount);
}
