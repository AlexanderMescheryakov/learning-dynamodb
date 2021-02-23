package com.trilogy.learning.market.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.PrintWriter;
import java.io.StringWriter;

@Data
@AllArgsConstructor
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
}
