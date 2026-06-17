package com.flurry.engine.plan;

import com.flurry.engine.parser.ast.Expr;

import java.util.List;

/**
 * Logical operators forming a query plan tree.
 * Sealed hierarchy: Scan (leaf) -> Filter -> Project.
 */
public sealed interface LogicalPlan
        permits LogicalPlan.Scan, LogicalPlan.Filter, LogicalPlan.Project {

    /** Leaf: read a table by name. */
    record Scan(String tableName) implements LogicalPlan {
        @Override public String toString() { return "Scan(" + tableName + ")"; }
    }

    /** Keep only rows where predicate is true. */
    record Filter(Expr predicate, LogicalPlan input) implements LogicalPlan {
        @Override public String toString() {
            return "Filter(" + predicate + ")\n  " + indent(input);
        }
    }

    /** Project a list of output columns/expressions. */
    record Project(List<ProjectItem> items, LogicalPlan input) implements LogicalPlan {
        @Override public String toString() {
            String cols = String.join(", ", items.stream().map(Object::toString).toList());
            return "Project([" + cols + "])\n  " + indent(input);
        }
    }

    /** One projected output: an expression with an output name. */
    record ProjectItem(Expr expr, String outputName) {
        @Override public String toString() {
            return expr + " AS " + outputName;
        }
    }

    private static String indent(LogicalPlan p) {
        return p.toString().replace("\n", "\n  ");
    }
}