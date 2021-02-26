package com.trilogy.learning.market.controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.transformers.v2.DynamodbEventTransformer;
import lombok.extern.jbosslog.JBossLog;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.OperationType;

import java.io.IOException;
import java.util.Map;

@JBossLog
abstract class AbstractDynamoStreamLambda implements RequestHandler<DynamodbEvent, Integer> {
    @Override
    public Integer handleRequest(final DynamodbEvent input, final Context context) {
        log.info(input);
        try {
            final var records = DynamodbEventTransformer.toRecordsV2(input);
            var processedRecordsCount = 0;
            for (var record : records) {
                final var oldItem = record.dynamodb().oldImage();
                final var newItem = record.dynamodb().newImage();
                final var operationType = record.eventName();
                try {
                    handle(oldItem, newItem, operationType);
                    processedRecordsCount++;
                } catch (Exception e) {
                    log.error("Event procession failed", e);
                }
            }

            return processedRecordsCount;
        } catch (Exception e) {
            log.error("Invocation failed", e);
            return 0;
        }
    }

    protected abstract void handle(Map<String, AttributeValue> oldItem,
                                   Map<String, AttributeValue> newItem,
                                   OperationType operationType) throws IOException;
}
