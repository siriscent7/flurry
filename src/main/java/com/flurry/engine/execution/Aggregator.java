package com.flurry.engine.execution;

/** Accumulates values for one aggregate function over a group. */
public final class Aggregator {

    public enum Func { COUNT, SUM, AVG, MIN, MAX }

    private final Func func;
    private long count = 0;
    private double sum = 0;
    private Object min = null;
    private Object max = null;

    public Aggregator(Func func) { this.func = func; }

    public void accept(Object value) {
        if (func == Func.COUNT) {
            if (value != null) count++;
            return;
        }
        if (value == null) return;
        count++;
        if (value instanceof Number n) sum += n.doubleValue();
        if (min == null || cmp(value, min) < 0) min = value;
        if (max == null || cmp(value, max) > 0) max = value;
    }

    public Object result() {
        return switch (func) {
            case COUNT -> count;
            case SUM   -> sum;
            case AVG   -> count == 0 ? null : sum / count;
            case MIN   -> min;
            case MAX   -> max;
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int cmp(Object a, Object b) {
        if (a instanceof Number x && b instanceof Number y)
            return Double.compare(x.doubleValue(), y.doubleValue());
        return ((Comparable) a).compareTo(b);
    }

    public static Func fromName(String name) {
        return switch (name.toUpperCase()) {
            case "COUNT" -> Func.COUNT;
            case "SUM"   -> Func.SUM;
            case "AVG"   -> Func.AVG;
            case "MIN"   -> Func.MIN;
            case "MAX"   -> Func.MAX;
            default -> throw new IllegalArgumentException("Unknown aggregate: " + name);
        };
    }
}