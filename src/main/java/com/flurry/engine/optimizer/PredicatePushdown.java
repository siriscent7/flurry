package com.flurry.engine.optimizer;

import com.flurry.engine.parser.ast.Expr;
import com.flurry.engine.plan.LogicalPlan;
import com.flurry.engine.storage.Catalog;
import com.flurry.engine.storage.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * Pushes a Filter below a Join when the predicate references columns from
 * only one side of the join, reducing rows that enter the join.
 */
public final class PredicatePushdown implements Rule {

    private final Catalog catalog;

    public PredicatePushdown(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override public String name() { return "PredicatePushdown"; }

    @Override public LogicalPlan apply(LogicalPlan plan) {
        return switch (plan) {
            case LogicalPlan.Filter f -> pushFilter(f);
            case LogicalPlan.Project p -> new LogicalPlan.Project(p.items(), apply(p.input()));
            case LogicalPlan.Aggregate a -> new LogicalPlan.Aggregate(a.groupBy(), a.items(), apply(a.input()));
            case LogicalPlan.SortLimit s -> new LogicalPlan.SortLimit(s.orderBy(), s.limit(), apply(s.input()));
            case LogicalPlan.Join j -> new LogicalPlan.Join(apply(j.left()), apply(j.right()), j.leftKey(), j.rightKey());
            case LogicalPlan.Scan s -> s;
        };
    }

    private LogicalPlan pushFilter(LogicalPlan.Filter f) {
        LogicalPlan input = apply(f.input());

        if (input instanceof LogicalPlan.Join j) {
            Set<String> cols = referencedColumns(f.predicate());
            Set<String> leftCols = columnsOf(j.left());
            Set<String> rightCols = columnsOf(j.right());

            if (leftCols.containsAll(cols)) {
                // push to left
                LogicalPlan newLeft = new LogicalPlan.Filter(f.predicate(), j.left());
                return new LogicalPlan.Join(apply(newLeft), j.right(), j.leftKey(), j.rightKey());
            }
            if (rightCols.containsAll(cols)) {
                // push to right
                LogicalPlan newRight = new LogicalPlan.Filter(f.predicate(), j.right());
                return new LogicalPlan.Join(j.left(), apply(newRight), j.leftKey(), j.rightKey());
            }
        }
        // can't push; keep filter on top of the (optimized) input
        return new LogicalPlan.Filter(f.predicate(), input);
    }

    private Set<String> referencedColumns(Expr expr) {
        Set<String> cols = new HashSet<>();
        collect(expr, cols);
        return cols;
    }

    private void collect(Expr expr, Set<String> out) {
        switch (expr) {
            case Expr.ColumnRef ref -> out.add(ref.column());
            case Expr.BinaryExpr b -> { collect(b.left(), out); collect(b.right(), out); }
            case Expr.UnaryExpr u -> collect(u.operand(), out);
            case Expr.FunctionCall fc -> fc.args().forEach(a -> collect(a, out));
            case Expr.Literal ignored -> { }
        }
    }

    private Set<String> columnsOf(LogicalPlan plan) {
        Set<String> cols = new HashSet<>();
        collectScanTables(plan, cols);
        return cols;
    }

    private void collectScanTables(LogicalPlan plan, Set<String> cols) {
        switch (plan) {
            case LogicalPlan.Scan s -> {
                Table t = catalog.require(s.tableName());
                t.schema().columns().forEach(c -> cols.add(c.name()));
            }
            case LogicalPlan.Filter f -> collectScanTables(f.input(), cols);
            case LogicalPlan.Project p -> collectScanTables(p.input(), cols);
            case LogicalPlan.Aggregate a -> collectScanTables(a.input(), cols);
            case LogicalPlan.SortLimit s -> collectScanTables(s.input(), cols);
            case LogicalPlan.Join j -> { collectScanTables(j.left(), cols); collectScanTables(j.right(), cols); }
        }
    }
}