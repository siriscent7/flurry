package com.flurry.engine;

import com.flurry.engine.execution.ExecutionEngine;
import com.flurry.engine.execution.Row;
import com.flurry.engine.optimizer.Optimizer;
import com.flurry.engine.parser.Lexer;
import com.flurry.engine.parser.Parser;
import com.flurry.engine.parser.Token;
import com.flurry.engine.parser.ast.SelectStatement;
import com.flurry.engine.plan.LogicalPlan;
import com.flurry.engine.plan.LogicalPlanner;
import com.flurry.engine.storage.Catalog;
import com.flurry.engine.storage.ColumnStats;
import com.flurry.engine.storage.CsvLoader;
import com.flurry.engine.storage.Table;

import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) { usage(); return; }

        switch (args[0].toLowerCase()) {
            case "lex" -> {
                String sql = args.length >= 2 ? args[1] : defaultSql();
                System.out.println("SQL: " + sql + "\n--- Tokens ---");
                for (Token t : new Lexer(sql).tokenize()) System.out.println(t);
            }
            case "parse" -> {
                String sql = args.length >= 2 ? args[1] : defaultSql();
                System.out.println("SQL: " + sql + "\n--- AST ---");
                System.out.println(Parser.parse(sql));
            }
            case "query" -> {
                if (args.length < 4) {
                    System.out.println("Usage: flurry query <table> <csv> \"<sql>\"");
                    return;
                }
                runQuery(args[1], Path.of(args[2]), args[3]);
            }
            case "query2" -> {
                if (args.length < 6) {
                    System.out.println("Usage: flurry query2 <t1> <csv1> <t2> <csv2> \"<sql>\"");
                    return;
                }
                runQuery2(args[1], Path.of(args[2]), args[3], Path.of(args[4]), args[5]);
            }
            case "explain" -> {
                if (args.length < 4) {
                    System.out.println("Usage: flurry explain <table> <csv> \"<sql>\"");
                    return;
                }
                explain(args[1], Path.of(args[2]), args[3]);
            }
            case "gen" -> {
                int rows = Integer.parseInt(args[1]);
                com.flurry.engine.util.DataGen.generate(rows, Path.of(args[2]));
            }
            case "shell" -> {
                // flurry shell <t1> <csv1> [<t2> <csv2> ...]
                if (args.length < 3 || (args.length - 1) % 2 != 0) {
                    System.out.println("Usage: flurry shell <t1> <csv1> [<t2> <csv2> ...]");
                    return;
                }
                Shell shell = new Shell();
                for (int i = 1; i + 1 < args.length; i += 2) {
                    shell.loadTable(args[i], Path.of(args[i + 1]));
                }
                shell.run();
            }
            default -> {
                if (args.length < 2) { usage(); return; }
                Catalog catalog = new Catalog();
                Table table = CsvLoader.load(args[0], Path.of(args[1]));
                catalog.register(table);
                System.out.println("Loaded table '" + table.name() + "'");
                System.out.println("Rows: " + table.rowCount());
                System.out.println("Columns: " + table.schema().columnCount());
                System.out.println("\n--- Column Statistics ---");
                for (ColumnStats s : table.allStats()) System.out.println(s);
            }
        }
    }

    private static void runQuery(String tableName, Path csv, String sql) throws Exception {
        Catalog catalog = new Catalog();
        catalog.register(CsvLoader.load(tableName, csv));
        runAndPrint(catalog, sql);
    }

    private static void runQuery2(String t1, Path csv1, String t2, Path csv2, String sql)
            throws Exception {
        Catalog catalog = new Catalog();
        catalog.register(CsvLoader.load(t1, csv1));
        catalog.register(CsvLoader.load(t2, csv2));
        runAndPrint(catalog, sql);
    }

    private static void explain(String tableName, Path csv, String sql) throws Exception {
        Catalog catalog = new Catalog();
        catalog.register(CsvLoader.load(tableName, csv));

        SelectStatement stmt = Parser.parse(sql);
        LogicalPlan unoptimized = new LogicalPlanner(catalog).plan(stmt);
        LogicalPlan optimized = new Optimizer(catalog).optimize(unoptimized);

        System.out.println("SQL: " + sql);
        System.out.println("\n=== BEFORE optimization ===");
        System.out.println(unoptimized);
        System.out.println("\n=== AFTER optimization ===");
        System.out.println(optimized);
    }

    private static void runAndPrint(Catalog catalog, String sql) {
        SelectStatement stmt = Parser.parse(sql);
        LogicalPlan plan = new LogicalPlanner(catalog).plan(stmt);
        LogicalPlan optimized = new Optimizer(catalog).optimize(plan);

        System.out.println("SQL:  " + sql);
        System.out.println("\n--- Optimized Plan ---");
        System.out.println(optimized);

        List<Row> rows = new ExecutionEngine(catalog).execute(optimized);
        System.out.println("\n--- Results (" + rows.size() + " rows) ---");
        for (Row r : rows) System.out.println(r);
    }

    private static String defaultSql() {
        return "SELECT name, age FROM users WHERE age >= 30 AND city = 'San Jose'";
    }

    private static void usage() {
        System.out.println("""
            Usage:
            flurry shell   <t1> <csv1> [<t2> <csv2> ...]    interactive SQL shell
            flurry lex     "<sql>"                          tokenize SQL
            flurry parse   "<sql>"                          parse SQL into an AST
            flurry query   <table> <csv> "<sql>"            run a query against one CSV
            flurry query2  <t1> <csv1> <t2> <csv2> "<sql>"  run a join query across two CSVs
            flurry explain <table> <csv> "<sql>"            show plan before/after optimization
            flurry gen <rows> <out>                         generate a benchmark CSV
            flurry <table> <csv>                            load a CSV and print column stats
            """);
    }
}