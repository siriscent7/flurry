package com.flurry.engine.plan;

import com.flurry.engine.parser.ast.Expr;
import com.flurry.engine.parser.ast.SelectStatement;
import com.flurry.engine.storage.Catalog;
import com.flurry.engine.storage.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Lowers a parsed SelectStatement into a LogicalPlan tree.
 *   SortLimit?( (Aggregate | Project)( Filter?( Join?( Scan ) ) ) )
 */
public final class LogicalPlanner {

    private final Catalog catalog;

    public LogicalPlanner(Catalog catalog) {
        this.catalog = catalog;
    }

    public LogicalPlan plan(SelectStatement stmt) {
        // 1. Scan (left table)
        Table leftTable = catalog.require(stmt.fromTable());
        LogicalPlan plan = new LogicalPlan.Scan(leftTable.name());

        // 2. Join (optional)
        if (stmt.join().isPresent()) {
            SelectStatement.JoinClause jc = stmt.join().get();
            Table rightTable = catalog.require(jc.rightTable());
            LogicalPlan rightScan = new LogicalPlan.Scan(rightTable.name());
            plan = new LogicalPlan.Join(plan, rightScan, jc.leftKey(), jc.rightKey());
        }

        // 3. Filter
        if (stmt.where().isPresent()) {
            plan = new LogicalPlan.Filter(stmt.where().get(), plan);
        }

        // 4. Aggregate vs Project
        boolean hasAggregate = !stmt.groupBy().isEmpty()
                || stmt.items().stream().anyMatch(i ->
                        !i.isStar() && i.expr() instanceof Expr.FunctionCall);

        if (hasAggregate) {
            plan = new LogicalPlan.Aggregate(stmt.groupBy(), stmt.items(), plan);
        } else {
            List<LogicalPlan.ProjectItem> projectItems = new ArrayList<>();
            for (SelectStatement.SelectItem item : stmt.items()) {
                if (item.isStar()) {
                    // expand * for the left table
                    for (var def : leftTable.schema().columns()) {
                        projectItems.add(new LogicalPlan.ProjectItem(
                                new Expr.ColumnRef(def.name()), def.name()));
                    }
                    // and the right table on a join (skip duplicate column names)
                    if (stmt.join().isPresent()) {
                        Table rightTable = catalog.require(stmt.join().get().rightTable());
                        for (var def : rightTable.schema().columns()) {
                            String name = def.name();
                            boolean dup = projectItems.stream()
                                    .anyMatch(pi -> pi.outputName().equals(name));
                            if (!dup) {
                                projectItems.add(new LogicalPlan.ProjectItem(
                                        new Expr.ColumnRef(name), name));
                            }
                        }
                    }
                } else {
                    String outName = item.alias() != null ? item.alias() : defaultName(item.expr());
                    projectItems.add(new LogicalPlan.ProjectItem(item.expr(), outName));
                }
            }
            plan = new LogicalPlan.Project(projectItems, plan);
        }

        // 5. Sort / Limit
        if (!stmt.orderBy().isEmpty() || stmt.limit().isPresent()) {
            plan = new LogicalPlan.SortLimit(stmt.orderBy(), stmt.limit().orElse(null), plan);
        }

        return plan;
    }

    private String defaultName(Expr expr) {
        if (expr instanceof Expr.ColumnRef ref) return ref.column();
        return expr.toString();
    }
}