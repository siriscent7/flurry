package com.flurry.engine.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CsvLoader {

    private static final int SAMPLE_ROWS = 100;

    public static Table load(String tableName, Path csvPath) throws IOException {
        List<String> lines = Files.readAllLines(csvPath);
        if (lines.isEmpty()) throw new IOException("Empty CSV: " + csvPath);

        String[] headers = split(lines.get(0));
        DataType[] types = new DataType[headers.length];

        int sampled = 0;
        for (int r = 1; r < lines.size() && sampled < SAMPLE_ROWS; r++, sampled++) {
            String[] cells = split(lines.get(r));
            for (int c = 0; c < headers.length && c < cells.length; c++) {
                if (cells[c] == null || cells[c].isBlank()) continue;      // ← add this line
                types[c] = widen(types[c], DataType.infer(cells[c]));
            }
        }

        List<Schema.ColumnDef> defs = new ArrayList<>();
        for (int c = 0; c < headers.length; c++) {
            defs.add(new Schema.ColumnDef(headers[c].trim(),
                    types[c] == null ? DataType.STRING : types[c]));
        }

        Table table = new Table(tableName, new Schema(defs));
        for (int r = 1; r < lines.size(); r++) {
            if (lines.get(r).isBlank()) continue;
            table.appendRow(split(lines.get(r)));
        }
        return table;
    }

    private static DataType widen(DataType current, DataType incoming) {
        if (current == null) return incoming;
        if (current == incoming) return current;
        boolean curNum = current == DataType.INT || current == DataType.LONG || current == DataType.DOUBLE;
        boolean inNum  = incoming == DataType.INT || incoming == DataType.LONG || incoming == DataType.DOUBLE;
        if (curNum && inNum) {
            if (current == DataType.DOUBLE || incoming == DataType.DOUBLE) return DataType.DOUBLE;
            if (current == DataType.LONG   || incoming == DataType.LONG)   return DataType.LONG;
            return DataType.INT;
        }
        return DataType.STRING;
    }

    private static String[] split(String line) { return line.split(",", -1); }

    private CsvLoader() {}
}