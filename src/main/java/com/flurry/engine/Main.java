package com.flurry.engine;

import com.flurry.engine.execution.ExecutionEngine;
import com.flurry.engine.execution.Row;
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
                // flurry query <table> <csv> "<sql>"
                if (args.length < 4) {
                    System.out.println("Usage: flurry query <table> <csv> \"<sql>\"");
                    return;
                }
                runQuery(args[1], Path.of(args[2]), args[3]);
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

        SelectStatement stmt = Parser.parse(sql);
        LogicalPlan plan = new LogicalPlanner(catalog).plan(stmt);

        System.out.println("SQL:  " + sql);
        System.out.println("\n--- Logical Plan ---");
        System.out.println(plan);

        List<Row> rows = new ExecutionEngine(catalog).execute(plan);
        System.out.println("\n--- Results (" + rows.size() + " rows) ---");
        for (Row r : rows) System.out.println(r);
    }

    private static String defaultSql() {
        return "SELECT name, age FROM users WHERE age >= 30 AND city = 'San Jose'";
    }

    private static void usage() {
        System.out.println("""
            Usage:
              flurry lex   "<sql>"                 tokenize SQL
              flurry parse "<sql>"                 parse SQL into an AST
              flurry query <table> <csv> "<sql>"   run a SQL query against a CSV
              flurry <table> <csv>                 load a CSV and print column stats
            """);
    }
}