package com.flurry.engine;

import com.flurry.engine.parser.Lexer;
import com.flurry.engine.parser.Token;
import com.flurry.engine.storage.Catalog;
import com.flurry.engine.storage.ColumnStats;
import com.flurry.engine.storage.CsvLoader;
import com.flurry.engine.storage.Table;

import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length >= 2 && !args[0].equalsIgnoreCase("lex")) {
            // CSV demo:  flurry <tableName> <csvPath>
            Catalog catalog = new Catalog();
            Table table = CsvLoader.load(args[0], Path.of(args[1]));
            catalog.register(table);

            System.out.println("Loaded table '" + table.name() + "'");
            System.out.println("Rows: " + table.rowCount());
            System.out.println("Columns: " + table.schema().columnCount());
            System.out.println("\n--- Column Statistics ---");
            for (ColumnStats s : table.allStats()) System.out.println(s);
            return;
        }

        // Lexer demo:  flurry lex "SELECT name FROM users WHERE age > 30"
        String sql = (args.length >= 2)
                ? args[1]
                : "SELECT name, age FROM users WHERE age >= 30 AND city = 'San Jose'";

        System.out.println("SQL: " + sql + "\n");
        System.out.println("--- Tokens ---");
        List<Token> tokens = new Lexer(sql).tokenize();
        for (Token t : tokens) System.out.println(t);
    }
}