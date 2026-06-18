package com.flurry.engine.optimizer;

import com.flurry.engine.plan.LogicalPlan;
import com.flurry.engine.storage.Catalog;

import java.util.List;

/** Applies a fixed pipeline of rewrite rules to a logical plan. */
public final class Optimizer {

    private final List<Rule> rules;

    public Optimizer(Catalog catalog) {
        this.rules = List.of(
            new ConstantFolding(),
            new PredicatePushdown(catalog)
        );
    }

    public LogicalPlan optimize(LogicalPlan plan) {
        LogicalPlan current = plan;
        for (Rule rule : rules) {
            current = rule.apply(current);
        }
        return current;
    }
}