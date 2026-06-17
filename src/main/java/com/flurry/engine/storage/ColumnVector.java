package com.flurry.engine.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ColumnVector {

    private final String name;
    private final DataType type;
    private final List<Object> values = new ArrayList<>();

    public ColumnVector(String name, DataType type) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
    }

    public String name() { return name; }
    public DataType type() { return type; }
    public int size() { return values.size(); }

    public void appendRaw(String raw) { values.add(coerce(raw)); }

    public Object get(int row) { return values.get(row); }

    private Object coerce(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim();
        return switch (type) {
            case INT     -> Integer.parseInt(s);
            case LONG    -> Long.parseLong(s);
            case DOUBLE  -> Double.parseDouble(s);
            case BOOLEAN -> Boolean.parseBoolean(s);
            case STRING  -> s;
        };
    }

    public ColumnStats stats() {
        long nulls = values.stream().filter(Objects::isNull).count();
        Object min = null, max = null;
        for (Object v : values) {
            if (v == null) continue;
            if (min == null || compare(v, min) < 0) min = v;
            if (max == null || compare(v, max) > 0) max = v;
        }
        return new ColumnStats(name, type, size(), nulls, min, max);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compare(Object a, Object b) {
        return ((Comparable) a).compareTo(b);
    }
}