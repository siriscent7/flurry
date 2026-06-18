package com.flurry.engine;

import com.flurry.engine.execution.ExecutionEngine;
import com.flurry.engine.execution.Row;
import com.flurry.engine.execution.TablePrinter;
import com.flurry.engine.optimizer.Optimizer;
import com.flurry.engine.parser.Parser;
import com.flurry.engine.parser.ast.SelectStatement;
import com.flurry.engine.plan.LogicalPlan;
import com.flurry.engine.plan.LogicalPlanner;
import com.flurry.engine.storage.Catalog;
import com.flurry.engine.storage.CsvLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

public final class Shell {

    private final Catalog catalog = new Catalog();

    public void loadTable(String name, Path csv) throws Exception {
        catalog.register(CsvLoader.load(name, csv));
    }

    public void run() throws Exception {
        printBanner();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        StringBuilder buffer = new StringBuilder();

        System.out.print("flurry> ");
        String line;
        while ((line = in.readLine()) != null) {
            String trimmed = line.trim();

            if (trimmed.equalsIgnoreCase("exit") || trimmed.equalsIgnoreCase("quit")) {
                System.out.println("Bye.");
                return;
            }
            if (trimmed.equalsIgnoreCase("tables")) {
                System.out.println(catalog.tableNames());
                System.out.print("flurry> ");
                continue;
            }
            if (trimmed.isEmpty()) {
                System.out.print("flurry> ");
                continue;
            }

            buffer.append(line).append(" ");

            if (trimmed.endsWith(";")) {
                String sql = buffer.toString().trim();
                sql = sql.substring(0, sql.length() - 1).trim();
                buffer.setLength(0);
                execute(sql);
                System.out.print("flurry> ");
            } else {
                System.out.print("    ..> ");
            }
        }
    }

    private void execute(String sql) {
        try {
            boolean explain = sql.toUpperCase().startsWith("EXPLAIN ");
            String actualSql = explain ? sql.substring("EXPLAIN ".length()).trim() : sql;

            long start = System.nanoTime();
            SelectStatement stmt = Parser.parse(actualSql);
            LogicalPlan plan = new LogicalPlanner(catalog).plan(stmt);
            LogicalPlan optimized = new Optimizer(catalog).optimize(plan);

            if (explain) {
                System.out.println();
                System.out.println("=== Logical Plan (optimized) ===");
                System.out.println(optimized);
                long ms = (System.nanoTime() - start) / 1_000_000;
                System.out.println("(planned in " + ms + " ms)");
                System.out.println();
                return;
            }

            List<Row> rows = new ExecutionEngine(catalog).execute(optimized);
            long ms = (System.nanoTime() - start) / 1_000_000;

            System.out.println();
            System.out.print(TablePrinter.format(rows));
            System.out.println(rows.size() + " row" + (rows.size() == 1 ? "" : "s")
                    + " (" + ms + " ms)");
            System.out.println();

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println();
        }
    }

    private void printBanner() {
        System.out.println("Flurry SQL Shell");
        System.out.println("Type SQL ending with ';'. Examples:");
        System.out.println("  SELECT city, COUNT(*) AS n FROM users GROUP BY city;");
        System.out.println("  EXPLAIN SELECT name FROM users WHERE age > 20 + 10;");
        System.out.println("Commands:  tables   exit");
        System.out.println();
    }
}