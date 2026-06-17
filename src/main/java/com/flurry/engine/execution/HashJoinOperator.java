package com.flurry.engine.execution;

import com.flurry.engine.parser.ast.Expr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Hash join (inner, equi-join).
 * Build phase: materialize the RIGHT input into a hash table keyed by rightKey.
 * Probe phase: stream the LEFT input, look up matches, emit merged rows.
 */
public final class HashJoinOperator implements Operator {

    private final Operator left;
    private final Operator right;
    private final Expr.ColumnRef leftKey;
    private final Expr.ColumnRef rightKey;

    private final Map<Object, List<Row>> buildSide = new HashMap<>();
    private Iterator<Row> outputIter;

    public HashJoinOperator(Operator left, Operator right,
                            Expr.ColumnRef leftKey, Expr.ColumnRef rightKey) {
        this.left = left;
        this.right = right;
        this.leftKey = leftKey;
        this.rightKey = rightKey;
    }

    @Override public void open() {
        // BUILD: hash the right side
        right.open();
        Row r;
        while ((r = right.next()) != null) {
            Object key = r.get(rightKey.column());
            buildSide.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }
        right.close();

        // PROBE: stream the left side, emit matches
        List<Row> output = new ArrayList<>();
        left.open();
        Row l;
        while ((l = left.next()) != null) {
            Object key = l.get(leftKey.column());
            List<Row> matches = buildSide.get(key);
            if (matches != null) {
                for (Row m : matches) {
                    output.add(merge(l, m));
                }
            }
        }
        left.close();

        outputIter = output.iterator();
    }

    private Row merge(Row left, Row right) {
        Row out = new Row();
        for (var e : left.values().entrySet()) out.put(e.getKey(), e.getValue());
        // right columns; don't overwrite existing keys (left wins on collision)
        for (var e : right.values().entrySet()) {
            if (!out.has(e.getKey())) out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    @Override public Row next() {
        return outputIter.hasNext() ? outputIter.next() : null;
    }

    @Override public void close() {}
}