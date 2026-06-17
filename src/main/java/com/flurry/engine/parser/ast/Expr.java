package com.flurry.engine.parser.ast;

import java.util.List;

public sealed interface Expr
        permits Expr.ColumnRef, Expr.Literal, Expr.BinaryExpr, Expr.UnaryExpr, Expr.FunctionCall {

    record ColumnRef(String table, String column) implements Expr {
        public ColumnRef(String column) { this(null, column); }
        @Override public String toString() {
            return table == null ? column : table + "." + column;
        }
    }

    record Literal(Object value, LiteralType type) implements Expr {
        public enum LiteralType { INTEGER, DOUBLE, STRING, BOOLEAN, NULL }
        @Override public String toString() {
            return type == LiteralType.STRING ? "'" + value + "'" : String.valueOf(value);
        }
    }

    record BinaryExpr(BinaryOp op, Expr left, Expr right) implements Expr {
        @Override public String toString() {
            return "(" + left + " " + op.symbol() + " " + right + ")";
        }
    }

    record UnaryExpr(UnaryOp op, Expr operand) implements Expr {
        @Override public String toString() {
            return "(" + op.symbol() + " " + operand + ")";
        }
    }

    /** Aggregate function call, e.g. COUNT(*), SUM(age). isStar=true for COUNT(*). */
    record FunctionCall(String name, List<Expr> args, boolean isStar) implements Expr {
        @Override public String toString() {
            return name + "(" + (isStar ? "*" : String.join(", ", args.stream().map(Object::toString).toList())) + ")";
        }
    }
}