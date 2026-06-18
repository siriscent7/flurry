package com.flurry.engine.optimizer;

import com.flurry.engine.parser.ast.BinaryOp;
import com.flurry.engine.parser.ast.Expr;
import com.flurry.engine.plan.LogicalPlan;

/** Folds constant sub-expressions, e.g. (50000 + 50000) -> 100000. */
public final class ConstantFolding implements Rule {

    @Override public String name() { return "ConstantFolding"; }

    @Override public LogicalPlan apply(LogicalPlan plan) {
        return switch (plan) {
            case LogicalPlan.Filter f -> new LogicalPlan.Filter(fold(f.predicate()), apply(f.input()));
            case LogicalPlan.Project p -> new LogicalPlan.Project(
                    p.items().stream().map(i ->
                        new LogicalPlan.ProjectItem(fold(i.expr()), i.outputName())).toList(),
                    apply(p.input()));
            case LogicalPlan.Aggregate a -> new LogicalPlan.Aggregate(a.groupBy(), a.items(), apply(a.input()));
            case LogicalPlan.SortLimit s -> new LogicalPlan.SortLimit(s.orderBy(), s.limit(), apply(s.input()));
            case LogicalPlan.Join j -> new LogicalPlan.Join(apply(j.left()), apply(j.right()), j.leftKey(), j.rightKey());
            case LogicalPlan.Scan s -> s;
        };
    }

    private Expr fold(Expr expr) {
        return switch (expr) {
            case Expr.BinaryExpr b -> {
                Expr l = fold(b.left());
                Expr r = fold(b.right());
                if (l instanceof Expr.Literal ll && r instanceof Expr.Literal rl
                        && isArithmetic(b.op())
                        && ll.value() instanceof Number && rl.value() instanceof Number) {
                    yield foldArithmetic(b.op(), (Number) ll.value(), (Number) rl.value());
                }
                yield new Expr.BinaryExpr(b.op(), l, r);
            }
            case Expr.UnaryExpr u -> new Expr.UnaryExpr(u.op(), fold(u.operand()));
            default -> expr;
        };
    }

    private boolean isArithmetic(BinaryOp op) {
        return op == BinaryOp.PLUS || op == BinaryOp.MINUS
                || op == BinaryOp.STAR || op == BinaryOp.SLASH;
    }

    private Expr foldArithmetic(BinaryOp op, Number a, Number b) {
        boolean ints = (a instanceof Integer && b instanceof Integer);
        double x = a.doubleValue(), y = b.doubleValue();
        double res = switch (op) {
            case PLUS -> x + y; case MINUS -> x - y;
            case STAR -> x * y; case SLASH -> x / y;
            default -> throw new IllegalStateException();
        };
        if (ints && op != BinaryOp.SLASH) {
            return new Expr.Literal((int) res, Expr.Literal.LiteralType.INTEGER);
        }
        return new Expr.Literal(res, Expr.Literal.LiteralType.DOUBLE);
    }
}