package com.flurry.engine.storage;

import java.util.List;

public record Schema(List<ColumnDef> columns) {

    public record ColumnDef(String name, DataType type) {}

    public int columnIndex(String name) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).name().equalsIgnoreCase(name)) return i;
        }
        throw new IllegalArgumentException("Unknown column: " + name);
    }

    public int columnCount() { return columns.size(); }
}