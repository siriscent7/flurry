package com.flurry.engine.execution;

import com.flurry.engine.plan.LogicalPlan;

import java.util.List;

/** Selects/computes output columns. */
public final class ProjectOperator implements Operator {

    private final List<LogicalPlan.ProjectItem> items;
    private final Operator input;

    public ProjectOperator(List<LogicalPlan.ProjectItem> items, Operator input) {
        this.items = items;
        this.input = input;
    }

    @Override public void open() { input.open(); }

    @Override public Row next() {
        Row in = input.next();
        if (in == null) return null;
        Row out = new Row();
        for (LogicalPlan.ProjectItem item : items) {
            out.put(item.outputName(), ExpressionEvaluator.eval(item.expr(), in));
        }
        return out;
    }

    @Override public void close() { input.close(); }
}