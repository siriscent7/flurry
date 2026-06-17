package com.flurry.engine.storage;

public record ColumnStats(
        String name, DataType type, int rowCount, long nullCount, Object min, Object max) {

    @Override
    public String toString() {
        return String.format(
            "%-15s | %-7s | rows=%-6d nulls=%-4d min=%-10s max=%-10s",
            name, type, rowCount, nullCount, String.valueOf(min), String.valueOf(max));
    }
}