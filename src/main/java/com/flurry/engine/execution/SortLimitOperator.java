package com.flurry.engine.execution;

import com.flurry.engine.parser.ast.SelectStatement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Materializes input, sorts by ORDER BY keys, applies LIMIT. */
public final class SortLimitOperator implements Operator {

    private final Operator input;
    private final List<SelectStatement.OrderKey> orderBy;
    private final Integer limit;
    private Iterator<Row> iter;

    public SortLimitOperator(List<SelectStatement.OrderKey> orderBy, Integer limit, Operator input) {
        this.orderBy = orderBy;
        this.limit = limit;
        this.input = input;
    }

    @Override public void open() {
        input.open();
        List<Row> rows = new ArrayList<>();
        Row row;
        while ((row = input.next()) != null) {
            rows.add(row);
        }

        if (!orderBy.isEmpty()) {
            rows.sort(this::compareRows);
        }
        if (limit != null && limit < rows.size()) {
            rows = new ArrayList<>(rows.subList(0, limit));
        }
        iter = rows.iterator();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compareRows(Row a, Row b) {
        for (SelectStatement.OrderKey key : orderBy) {
            Object va = ExpressionEvaluator.eval(key.expr(), a);
            Object vb = ExpressionEvaluator.eval(key.expr(), b);
            int c;
            if (va == null && vb == null) c = 0;
            else if (va == null) c = -1;
            else if (vb == null) c = 1;
            else if (va instanceof Number x && vb instanceof Number y)
                c = Double.compare(x.doubleValue(), y.doubleValue());
            else c = ((Comparable) va).compareTo(vb);

            if (c != 0) return key.descending() ? -c : c;
        }
        return 0;
    }

    @Override public Row next() {
        return iter.hasNext() ? iter.next() : null;
    }

    @Override public void close() { input.close(); }
}