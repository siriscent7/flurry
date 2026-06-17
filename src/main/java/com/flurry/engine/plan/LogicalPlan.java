package com.flurry.engine.plan;

import com.flurry.engine.parser.ast.Expr;
import com.flurry.engine.parser.ast.SelectStatement;

import java.util.List;

public sealed interface LogicalPlan
        permits LogicalPlan.Scan, LogicalPlan.Filter, LogicalPlan.Project,
                LogicalPlan.Aggregate, LogicalPlan.SortLimit, LogicalPlan.Join {

    record Scan(String tableName) implements LogicalPlan {
        @Override public String toString() { return "Scan(" + tableName + ")"; }
    }

    record Filter(Expr predicate, LogicalPlan input) implements LogicalPlan {
        @Override public String toString() {
            return "Filter[" + predicate + "]\n  " + indent(input);
        }
    }

    record Project(List<ProjectItem> items, LogicalPlan input) implements LogicalPlan {
        @Override public String toString() {
            String cols = String.join(", ", items.stream().map(Object::toString).toList());
            return "Project([" + cols + "])\n  " + indent(input);
        }
    }

    record ProjectItem(Expr expr, String outputName) {
        @Override public String toString() { return expr + " AS " + outputName; }
    }

    record Aggregate(List<Expr> groupBy,
                     List<SelectStatement.SelectItem> items,
                     LogicalPlan input) implements LogicalPlan {
        @Override public String toString() { return "Aggregate\n  " + indent(input); }
    }

    record SortLimit(List<SelectStatement.OrderKey> orderBy,
                     Integer limit,
                     LogicalPlan input) implements LogicalPlan {
        @Override public String toString() { return "SortLimit\n  " + indent(input); }
    }

    record Join(LogicalPlan left, LogicalPlan right,
                Expr.ColumnRef leftKey, Expr.ColumnRef rightKey) implements LogicalPlan {
        @Override public String toString() {
            return "HashJoin[" + leftKey + " = " + rightKey + "]\n  "
                    + indent(left) + "\n  " + indent(right);
        }
    }

    private static String indent(LogicalPlan p) {
        return p.toString().replace("\n", "\n  ");
    }
}