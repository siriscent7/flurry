package com.flurry.engine.parser.ast;

/**
 * Expression nodes for WHERE clauses and SELECT items.
 * Sealed so the compiler knows all variants (great for exhaustive switches later).
 */
public sealed interface Expr
        permits Expr.ColumnRef, Expr.Literal, Expr.BinaryExpr, Expr.UnaryExpr {

    /** A reference to a column, e.g. `age` or `users.age`. */
    record ColumnRef(String table, String column) implements Expr {
        public ColumnRef(String column) { this(null, column); }
        @Override public String toString() {
            return table == null ? column : table + "." + column;
        }
    }

    /** A constant literal: integer, double, string, boolean, or null. */
    record Literal(Object value, LiteralType type) implements Expr {
        public enum LiteralType { INTEGER, DOUBLE, STRING, BOOLEAN, NULL }
        @Override public String toString() {
            return type == LiteralType.STRING ? "'" + value + "'" : String.valueOf(value);
        }
    }

    /** A binary operation, e.g. `age >= 30` or `a AND b`. */
    record BinaryExpr(BinaryOp op, Expr left, Expr right) implements Expr {
        @Override public String toString() {
            return "(" + left + " " + op.symbol() + " " + right + ")";
        }
    }

    /** A unary operation, e.g. `NOT active` or `-price`. */
    record UnaryExpr(UnaryOp op, Expr operand) implements Expr {
        @Override public String toString() {
            return "(" + op.symbol() + " " + operand + ")";
        }
    }
}