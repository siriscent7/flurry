package com.flurry.engine.execution;

import com.flurry.engine.parser.ast.Expr;
import com.flurry.engine.parser.ast.SelectStatement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Implements GROUP BY with aggregates. */
public final class AggregateOperator implements Operator {

    private final Operator input;
    private final List<Expr> groupBy;
    private final List<SelectStatement.SelectItem> selectItems;

    private Iterator<Row> resultIter;

    public AggregateOperator(List<Expr> groupBy,
                             List<SelectStatement.SelectItem> selectItems,
                             Operator input) {
        this.groupBy = groupBy;
        this.selectItems = selectItems;
        this.input = input;
    }

    @Override public void open() {
        input.open();

        Map<List<Object>, List<Aggregator>> groups = new LinkedHashMap<>();

        Row row;
        while ((row = input.next()) != null) {
            List<Object> key = new ArrayList<>();
            for (Expr g : groupBy) key.add(ExpressionEvaluator.eval(g, row));

            List<Aggregator> aggs = groups.computeIfAbsent(key, k -> newAggregators());

            int ai = 0;
            for (SelectStatement.SelectItem item : selectItems) {
                if (item.expr() instanceof Expr.FunctionCall fc) {
                    Object v = fc.isStar()
                            ? Boolean.TRUE
                            : ExpressionEvaluator.eval(fc.args().get(0), row);
                    aggs.get(ai++).accept(v);
                }
            }
        }

        List<Row> out = new ArrayList<>();
        for (var entry : groups.entrySet()) {
            Row outRow = new Row();
            int gi = 0, ai = 0;
            List<Aggregator> aggs = entry.getValue();
            for (SelectStatement.SelectItem item : selectItems) {
                String col = columnLabel(item);
                if (item.expr() instanceof Expr.FunctionCall) {
                    outRow.put(col, aggs.get(ai++).result());
                } else {
                    outRow.put(col, entry.getKey().get(gi++));
                }
            }
            out.add(outRow);
        }
        resultIter = out.iterator();
    }

    private List<Aggregator> newAggregators() {
        List<Aggregator> list = new ArrayList<>();
        for (SelectStatement.SelectItem item : selectItems) {
            if (item.expr() instanceof Expr.FunctionCall fc) {
                list.add(new Aggregator(Aggregator.fromName(fc.name())));
            }
        }
        return list;
    }

    private String columnLabel(SelectStatement.SelectItem item) {
        if (item.alias() != null) return item.alias();
        return item.expr().toString();
    }

    @Override public Row next() {
        return resultIter.hasNext() ? resultIter.next() : null;
    }

    @Override public void close() { input.close(); }
}