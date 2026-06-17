package com.flurry.engine.parser;

/** Thrown when the lexer encounters input it cannot tokenize. */
public class LexException extends RuntimeException {
    public LexException(String message, int pos) {
        super("Lex error at position " + pos + ": " + message);
    }
}