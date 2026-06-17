package com.flurry.engine.execution;

import com.flurry.engine.parser.ast.BinaryOp;
import com.flurry.engine.parser.ast.Expr;

/** Evaluates an expression tree against a single Row. */
public final class ExpressionEvaluator {

    public static Object eval(Expr expr, Row row) {
        return switch (expr) {
            case Expr.Literal lit   -> lit.value();
            case Expr.ColumnRef ref -> row.get(ref.column());
            case Expr.UnaryExpr u   -> evalUnary(u, row);
            case Expr.BinaryExpr b  -> evalBinary(b, row);
            case Expr.FunctionCall fc -> throw new IllegalStateException(
                    "Aggregate function " + fc.name() + " cannot be evaluated per-row");
        };
    }

    public static boolean evalPredicate(Expr expr, Row row) {
        Object v = eval(expr, row);
        return Boolean.TRUE.equals(v);
    }

    private static Object evalUnary(Expr.UnaryExpr u, Row row) {
        Object v = eval(u.operand(), row);
        return switch (u.op()) {
            case NOT -> !Boolean.TRUE.equals(v);
            case NEG -> negate(v);
        };
    }

    private static Object negate(Object v) {
        if (v instanceof Integer i) return -i;
        if (v instanceof Long l)    return -l;
        if (v instanceof Double d)  return -d;
        throw new IllegalArgumentException("Cannot negate: " + v);
    }

    private static Object evalBinary(Expr.BinaryExpr b, Row row) {
        if (b.op() == BinaryOp.AND) {
            return evalPredicate(b.left(), row) && evalPredicate(b.right(), row);
        }
        if (b.op() == BinaryOp.OR) {
            return evalPredicate(b.left(), row) || evalPredicate(b.right(), row);
        }

        Object l = eval(b.left(), row);
        Object r = eval(b.right(), row);

        boolean comparison = switch (b.op()) {
            case EQ, NEQ, LT, LTE, GT, GTE -> true;
            default -> false;
        };
        if (comparison && (l == null || r == null)) {
            return false;
        }

        return switch (b.op()) {
            case EQ  -> equalsNullSafe(l, r);
            case NEQ -> !equalsNullSafe(l, r);
            case LT  -> compare(l, r) < 0;
            case LTE -> compare(l, r) <= 0;
            case GT  -> compare(l, r) > 0;
            case GTE -> compare(l, r) >= 0;
            case PLUS  -> arith(l, r, '+');
            case MINUS -> arith(l, r, '-');
            case STAR  -> arith(l, r, '*');
            case SLASH -> arith(l, r, '/');
            default -> throw new IllegalStateException("Unsupported op: " + b.op());
        };
    }

    private static boolean equalsNullSafe(Object a, Object b) {
        if (a == null || b == null) return false;
        if (isNumber(a) && isNumber(b)) return toDouble(a) == toDouble(b);
        return a.equals(b);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compare(Object a, Object b) {
        if (isNumber(a) && isNumber(b)) return Double.compare(toDouble(a), toDouble(b));
        return ((Comparable) a).compareTo(b);
    }

    private static Object arith(Object a, Object b, char op) {
        double x = toDouble(a), y = toDouble(b);
        double result = switch (op) {
            case '+' -> x + y;
            case '-' -> x - y;
            case '*' -> x * y;
            case '/' -> x / y;
            default  -> throw new IllegalStateException();
        };
        if (a instanceof Integer && b instanceof Integer && op != '/') return (int) result;
        return result;
    }

    private static boolean isNumber(Object o) {
        return o instanceof Integer || o instanceof Long || o instanceof Double;
    }

    private static double toDouble(Object o) {
        return ((Number) o).doubleValue();
    }

    private ExpressionEvaluator() {}
}