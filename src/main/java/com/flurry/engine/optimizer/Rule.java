package com.flurry.engine.optimizer;

import com.flurry.engine.plan.LogicalPlan;

/** A plan rewrite rule. */
public interface Rule {
    String name();
    LogicalPlan apply(LogicalPlan plan);
}