package com.flurry.engine.execution;

import com.flurry.engine.plan.LogicalPlan;
import com.flurry.engine.storage.Catalog;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates a LogicalPlan into a tree of physical Operators and executes it,
 * collecting all result rows.
 */
public final class ExecutionEngine {

    private final Catalog catalog;

    public ExecutionEngine(Catalog catalog) {
        this.catalog = catalog;
    }

    public List<Row> execute(LogicalPlan plan) {
        Operator root = build(plan);
        List<Row> results = new ArrayList<>();
        root.open();
        try {
            Row row;
            while ((row = root.next()) != null) {
                results.add(row);
            }
        } finally {
            root.close();
        }
        return results;
    }

    private Operator build(LogicalPlan plan) {
        return switch (plan) {
            case LogicalPlan.Scan s ->
                new ScanOperator(catalog.require(s.tableName()));
            case LogicalPlan.Filter f ->
                new FilterOperator(f.predicate(), build(f.input()));
            case LogicalPlan.Project p ->
                new ProjectOperator(p.items(), build(p.input()));
            case LogicalPlan.Aggregate a ->
                new AggregateOperator(a.groupBy(), a.items(), build(a.input()));
            case LogicalPlan.SortLimit s ->
                new SortLimitOperator(s.orderBy(), s.limit(), build(s.input()));
            case LogicalPlan.Join j ->
                new HashJoinOperator(build(j.left()), build(j.right()), j.leftKey(), j.rightKey());
        };
    }
}