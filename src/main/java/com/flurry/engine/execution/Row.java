package com.flurry.engine.execution;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single output/intermediate row: ordered column-name -> value.
 * LinkedHashMap preserves column order for printing.
 */
public final class Row {

    private final Map<String, Object> values = new LinkedHashMap<>();

    public void put(String column, Object value) { values.put(column, value); }

    public Object get(String column) {
        if (!values.containsKey(column)) {
            throw new IllegalArgumentException("No such column in row: " + column);
        }
        return values.get(column);
    }

    public boolean has(String column) { return values.containsKey(column); }

    public Map<String, Object> values() { return values; }

    @Override public String toString() { return values.toString(); }
}