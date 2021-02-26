package com.trilogy.learning.market.repository.dynamodb;

import com.trilogy.learning.market.model.Payment;
import com.trilogy.learning.market.repository.IPaymentRepository;
import lombok.extern.jbosslog.JBossLog;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

@JBossLog
@Singleton
public class PaymentRepository implements IPaymentRepository {
    private static final String KEY_PREFIX = "PAYMENT#";
    private static final String PK_ATTRIBUTE= "PK";
    private static final String SK_ATTRIBUTE = "SK";
    private static final String ID_ATTRIBUTE = PK_ATTRIBUTE;
    private static final String DATE_ATTRIBUTE = SK_ATTRIBUTE;
    private static final String AMOUNT_ATTRIBUTE = "Amount";
    private static final PrefixAttributeConverter idConverter = new PrefixAttributeConverter(KEY_PREFIX);
    private static final StaticTableSchema<Payment> TABLE_SCHEMA =
            StaticTableSchema.builder(Payment.class)
                    .newItemSupplier(Payment::new)
                    .addAttribute(String.class, a -> a.name(ID_ATTRIBUTE)
                            .getter(Payment::getCustomerId)
                            .setter(Payment::setCustomerId)
                            .tags(primaryPartitionKey())
                            .attributeConverter(idConverter))
                    .addAttribute(LocalDateTime.class, a -> a.name(DATE_ATTRIBUTE)
                            .getter(Payment::getDate)
                            .setter(Payment::setDate)
                            .tags(primarySortKey()))
                    .addAttribute(BigDecimal.class, a -> a.name(AMOUNT_ATTRIBUTE)
                            .getter(Payment::getAmount)
                            .setter(Payment::setAmount))
                    .build();

    private final DynamoDbTable<Payment> table;

    @Inject
    public PaymentRepository(DynamoDbClient dynamoDbClient) {
        final var enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
        table = enhancedClient.table(getTableName(), TABLE_SCHEMA);
    }

    public String getTableName() {
        return System.getenv("DYNAMODB_TABLE_PAYMENTS");
    }

    @Override
    public TransactWriteItem getAddPaymentTransactRequest(String customerId, BigDecimal amount) {
        final var payment = new Payment(customerId, LocalDateTime.now(), amount);
        final var item = TABLE_SCHEMA.itemToMap(payment, true);
        final var putRequest = Put.builder()
                .item(item)
                .tableName(getTableName())
                .build();
        return TransactWriteItem.builder().put(putRequest).build();
    }

    @Override
    public List<Payment> getByCustomerId(String customerId) {
        final var pk = getPartitionKey(customerId);
        final var payments = new ArrayList<Payment>();
        table.query(keyEqualTo(k -> k.partitionValue(pk))).stream()
                .forEach(p -> payments.addAll(p.items()));
        log.info(payments);
        return payments;
    }

    private String getPartitionKey(String id) {
        return KEY_PREFIX + id;
    }
}
