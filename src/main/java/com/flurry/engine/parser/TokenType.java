package com.flurry.engine.parser;

public enum TokenType {
    // Keywords
    SELECT, FROM, WHERE, AND, OR, NOT,
    JOIN, ON, GROUP, BY, ORDER, ASC, DESC, LIMIT,
    AS, NULL, TRUE, FALSE,

    // Identifiers & literals
    IDENTIFIER,        // column / table names
    INTEGER_LITERAL,   // 42
    DOUBLE_LITERAL,    // 3.14
    STRING_LITERAL,    // 'hello'

    // Operators
    EQ,                // =
    NEQ,               // != or <>
    LT,                // <
    LTE,               // <=
    GT,                // >
    GTE,               // >=
    PLUS,              // +
    MINUS,             // -
    STAR,              // *
    SLASH,             // /

    // Punctuation
    COMMA,             // ,
    LPAREN,            // (
    RPAREN,            // )
    DOT,               // .

    EOF                // end of input
}