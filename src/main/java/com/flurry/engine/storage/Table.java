package com.flurry.engine.storage;

import java.util.ArrayList;
import java.util.List;

public final class Table {

    private final String name;
    private final Schema schema;
    private final List<ColumnVector> columns = new ArrayList<>();

    public Table(String name, Schema schema) {
        this.name = name;
        this.schema = schema;
        for (Schema.ColumnDef def : schema.columns()) {
            columns.add(new ColumnVector(def.name(), def.type()));
        }
    }

    public String name() { return name; }
    public Schema schema() { return schema; }
    public ColumnVector column(int i) { return columns.get(i); }
    public ColumnVector column(String name) { return columns.get(schema.columnIndex(name)); }

    public int rowCount() { return columns.isEmpty() ? 0 : columns.get(0).size(); }

    public void appendRow(String[] cells) {
        if (cells.length != columns.size()) {
            throw new IllegalArgumentException(
                "Row width " + cells.length + " != schema width " + columns.size());
        }
        for (int i = 0; i < cells.length; i++) {
            columns.get(i).appendRaw(cells[i]);
        }
    }

    public List<ColumnStats> allStats() {
        return columns.stream().map(ColumnVector::stats).toList();
    }
}