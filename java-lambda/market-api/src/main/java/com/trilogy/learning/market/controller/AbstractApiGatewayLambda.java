package com.trilogy.learning.market.controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.trilogy.learning.market.model.ErrorMessage;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.TreeMap;

abstract class AbstractApiGatewayLambda
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        final var params = input.getQueryStringParameters();
        final var caseInsensitiveParams = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        if (params != null) {
            caseInsensitiveParams.putAll(params);
        }

        try {
            final var result = handle(caseInsensitiveParams, input.getBody());
            if (result != null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(HttpURLConnection.HTTP_OK)
                        .withBody(result);
            }
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
                    .withBody(ErrorMessage.asJson(e));
        }

        return new APIGatewayProxyResponseEvent().withStatusCode(HttpURLConnection.HTTP_NOT_FOUND);
    }

    protected abstract String handle(Map<String, String> queryParams, String body) throws IOException;
}
