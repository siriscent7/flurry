package com.flurry.engine.execution;

import com.flurry.engine.storage.Table;

/** Reads all rows from a Table, reconstructing each row from columnar storage. */
public final class ScanOperator implements Operator {

    private final Table table;
    private int currentRow = 0;

    public ScanOperator(Table table) {
        this.table = table;
    }

    @Override public void open() { currentRow = 0; }

    @Override public Row next() {
        if (currentRow >= table.rowCount()) return null;
        Row row = new Row();
        for (var def : table.schema().columns()) {
            row.put(def.name(), table.column(def.name()).get(currentRow));
        }
        currentRow++;
        return row;
    }

    @Override public void close() {}
}