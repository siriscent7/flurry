package com.flurry.engine.plan;

import com.flurry.engine.parser.ast.Expr;
import com.flurry.engine.parser.ast.SelectStatement;
import com.flurry.engine.storage.Catalog;
import com.flurry.engine.storage.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Lowers a parsed SelectStatement into a LogicalPlan tree:
 *   Project( Filter?( Scan ) )
 */
public final class LogicalPlanner {

    private final Catalog catalog;

    public LogicalPlanner(Catalog catalog) {
        this.catalog = catalog;
    }

    public LogicalPlan plan(SelectStatement stmt) {
        // 1. Scan
        Table table = catalog.require(stmt.fromTable());
        LogicalPlan plan = new LogicalPlan.Scan(table.name());

        // 2. Filter (if WHERE present)
        if (stmt.where().isPresent()) {
            plan = new LogicalPlan.Filter(stmt.where().get(), plan);
        }

        // 3. Project
        List<LogicalPlan.ProjectItem> projectItems = new ArrayList<>();
        for (SelectStatement.SelectItem item : stmt.items()) {
            if (item.isStar()) {
                // expand * into all columns of the table
                for (var def : table.schema().columns()) {
                    projectItems.add(new LogicalPlan.ProjectItem(
                            new Expr.ColumnRef(def.name()), def.name()));
                }
            } else {
                String outName = item.alias() != null ? item.alias() : defaultName(item.expr());
                projectItems.add(new LogicalPlan.ProjectItem(item.expr(), outName));
            }
        }
        return new LogicalPlan.Project(projectItems, plan);
    }

    private String defaultName(Expr expr) {
        if (expr instanceof Expr.ColumnRef ref) return ref.column();
        return expr.toString();
    }
}