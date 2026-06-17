package com.flurry.engine.execution;

import com.flurry.engine.parser.Parser;
import com.flurry.engine.parser.ast.SelectStatement;
import com.flurry.engine.plan.LogicalPlan;
import com.flurry.engine.plan.LogicalPlanner;
import com.flurry.engine.storage.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExecutionTest {

    private Catalog catalog;

    @BeforeEach
    void setup() {
        // Build an in-memory "users" table without needing a CSV file.
        Schema schema = new Schema(List.of(
            new Schema.ColumnDef("id", DataType.INT),
            new Schema.ColumnDef("name", DataType.STRING),
            new Schema.ColumnDef("age", DataType.INT),
            new Schema.ColumnDef("city", DataType.STRING)
        ));
        Table t = new Table("users", schema);
        t.appendRow(new String[]{"1", "Alice", "30", "San Jose"});
        t.appendRow(new String[]{"2", "Bob",   "25", "Seattle"});
        t.appendRow(new String[]{"3", "Carol", "40", "San Jose"});
        t.appendRow(new String[]{"4", "Dave",  "35", "Austin"});

        catalog = new Catalog();
        catalog.register(t);
    }

    private List<Row> run(String sql) {
        SelectStatement stmt = Parser.parse(sql);
        LogicalPlan plan = new LogicalPlanner(catalog).plan(stmt);
        return new ExecutionEngine(catalog).execute(plan);
    }

    @Test
    void selectAllColumns() {
        List<Row> rows = run("SELECT * FROM users");
        assertEquals(4, rows.size());
        assertEquals("Alice", rows.get(0).get("name"));
    }

    @Test
    void projectSubsetOfColumns() {
        List<Row> rows = run("SELECT name, age FROM users");
        assertEquals(4, rows.size());
        assertEquals(2, rows.get(0).values().size());
        assertTrue(rows.get(0).has("name"));
        assertTrue(rows.get(0).has("age"));
        assertFalse(rows.get(0).has("city"));
    }

    @Test
    void filterWithComparison() {
        List<Row> rows = run("SELECT name FROM users WHERE age >= 35");
        assertEquals(2, rows.size()); // Carol (40), Dave (35)
    }

    @Test
    void filterWithAnd() {
        List<Row> rows = run("SELECT name FROM users WHERE age >= 30 AND city = 'San Jose'");
        assertEquals(2, rows.size()); // Alice, Carol
    }

    @Test
    void filterWithOr() {
        List<Row> rows = run("SELECT name FROM users WHERE city = 'Seattle' OR city = 'Austin'");
        assertEquals(2, rows.size()); // Bob, Dave
    }

    @Test
    void arithmeticInProjection() {
        List<Row> rows = run("SELECT name, age + 1 AS next_age FROM users WHERE name = 'Alice'");
        assertEquals(1, rows.size());
        assertEquals(31, rows.get(0).get("next_age"));
    }
}