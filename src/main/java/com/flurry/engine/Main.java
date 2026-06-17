package com.flurry.engine;

import com.flurry.engine.storage.Catalog;
import com.flurry.engine.storage.ColumnStats;
import com.flurry.engine.storage.CsvLoader;
import com.flurry.engine.storage.Table;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: flurry <tableName> <csvPath>");
            return;
        }
        Catalog catalog = new Catalog();
        Table table = CsvLoader.load(args[0], Path.of(args[1]));
        catalog.register(table);

        System.out.println("Loaded table '" + table.name() + "'");
        System.out.println("Rows: " + table.rowCount());
        System.out.println("Columns: " + table.schema().columnCount());
        System.out.println("\n--- Column Statistics ---");
        for (ColumnStats s : table.allStats()) {
            System.out.println(s);
        }
    }
}