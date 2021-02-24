package com.trilogy.learning.market.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.PrintWriter;
import java.io.StringWriter;

@Data
@AllArgsConstructor
@RegisterForReflection
public class ErrorMessage {
    private String message;
    private String stackTrace;

    public ErrorMessage(Exception e) {
        message = e.getMessage();
        var stream = new StringWriter();
        var writer = new PrintWriter(stream);
        e.printStackTrace(writer);
        stackTrace = stream.toString();
    }

    public static String asJson(Exception e) {
        final var objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(new ErrorMessage(e));
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"unknown\"}";
        }
    }
}
