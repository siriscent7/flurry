package com.flurry.engine.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class Catalog {

    private final Map<String, Table> tables = new HashMap<>();

    public void register(Table table) { tables.put(table.name().toLowerCase(), table); }

    public Optional<Table> lookup(String name) {
        return Optional.ofNullable(tables.get(name.toLowerCase()));
    }

    public Table require(String name) {
        return lookup(name).orElseThrow(
            () -> new IllegalArgumentException("Table not found: " + name));
    }
}