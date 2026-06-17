package com.flurry.engine.parser;

public class ParseException extends RuntimeException {
    public ParseException(String message, int pos) {
        super("Parse error at position " + pos + ": " + message);
    }
}