package com.flurry.engine.execution;

import com.flurry.engine.parser.ast.Expr;

/** Passes through only rows whose predicate evaluates true. */
public final class FilterOperator implements Operator {

    private final Expr predicate;
    private final Operator input;

    public FilterOperator(Expr predicate, Operator input) {
        this.predicate = predicate;
        this.input = input;
    }

    @Override public void open() { input.open(); }

    @Override public Row next() {
        Row row;
        while ((row = input.next()) != null) {
            if (ExpressionEvaluator.evalPredicate(predicate, row)) return row;
        }
        return null;
    }

    @Override public void close() { input.close(); }
}