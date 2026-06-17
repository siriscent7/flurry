package com.flurry.engine.parser.ast;

public enum BinaryOp {
    OR("OR"), AND("AND"),
    EQ("="), NEQ("!="), LT("<"), LTE("<="), GT(">"), GTE(">="),
    PLUS("+"), MINUS("-"), STAR("*"), SLASH("/");

    private final String symbol;
    BinaryOp(String symbol) { this.symbol = symbol; }
    public String symbol() { return symbol; }
}