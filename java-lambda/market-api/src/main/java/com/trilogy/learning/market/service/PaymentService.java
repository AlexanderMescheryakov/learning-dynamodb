package com.trilogy.learning.market.service;

import com.trilogy.learning.market.model.Order;
import com.trilogy.learning.market.model.Payment;
import com.trilogy.learning.market.repository.IOrderRepository;
import com.trilogy.learning.market.repository.IPaymentRepository;
import com.trilogy.learning.market.repository.ITransactionService;
import lombok.AllArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import javax.inject.Singleton;
import java.math.BigDecimal;
import java.security.InvalidParameterException;
import java.util.ArrayList;

import static java.lang.String.format;

@JBossLog
@Singleton
@AllArgsConstructor
public class PaymentService implements IPaymentService {
    private final IOrderRepository orderRepository;
    private final IPaymentRepository paymentRepository;
    private final ITransactionService transactionService;

    @Override
    public Payment.Status handleOrderPayment(String customerId, String orderId, BigDecimal amount) {
        final var oldOrder = orderRepository.getById(orderId);
        if (oldOrder == null) {
            throw new InvalidParameterException(format("Order not found for ID=%s", orderId));
        }

        if (Order.Status.PAID.equals(oldOrder.getStatus())) {
            return Payment.Status.SKIPPED;
        }

        final var requests = new ArrayList<TransactWriteItem>();
        final var addPaymentRequest = paymentRepository.getAddPaymentTransactRequest(customerId, amount);
        requests.add(addPaymentRequest);
        final var changeStatusRequest = orderRepository.getChangeStatusTransactRequest(
                customerId, orderId, oldOrder, Order.Status.PAID);
        requests.addAll(changeStatusRequest);
        try {
            transactionService.commit(requests);
            return Payment.Status.SUCCESS;
        } catch (TransactionCanceledException e) {
            return Payment.Status.NOT_ALLOWERED;
        }
    }
}
