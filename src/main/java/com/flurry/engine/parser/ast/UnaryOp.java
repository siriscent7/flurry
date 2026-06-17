package com.flurry.engine.parser.ast;

public enum UnaryOp {
    NOT("NOT"), NEG("-");

    private final String symbol;
    UnaryOp(String symbol) { this.symbol = symbol; }
    public String symbol() { return symbol; }
}