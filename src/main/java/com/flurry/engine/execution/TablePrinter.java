package com.flurry.engine.execution;

import java.util.ArrayList;
import java.util.List;

/** Pretty-prints a list of Rows as an aligned ASCII table. */
public final class TablePrinter {

    public static String format(List<Row> rows) {
        if (rows.isEmpty()) return "(0 rows)";

        // Column order from the first row
        List<String> cols = new ArrayList<>(rows.get(0).values().keySet());

        // Compute max width per column
        int[] widths = new int[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            widths[i] = cols.get(i).length();
        }
        for (Row r : rows) {
            int i = 0;
            for (String c : cols) {
                String val = String.valueOf(r.values().get(c));
                widths[i] = Math.max(widths[i], val.length());
                i++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(separator(widths));
        sb.append(rowLine(cols, widths));
        sb.append(separator(widths));
        for (Row r : rows) {
            List<String> cells = new ArrayList<>();
            for (String c : cols) cells.add(String.valueOf(r.values().get(c)));
            sb.append(rowLine(cells, widths));
        }
        sb.append(separator(widths));
        return sb.toString();
    }

    private static String separator(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            sb.append("-".repeat(w + 2)).append("+");
        }
        return sb.append("\n").toString();
    }

    private static String rowLine(List<String> cells, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < cells.size(); i++) {
            sb.append(" ").append(pad(cells.get(i), widths[i])).append(" |");
        }
        return sb.append("\n").toString();
    }

    private static String pad(String s, int width) {
        return s + " ".repeat(Math.max(0, width - s.length()));
    }

    private TablePrinter() {}
}