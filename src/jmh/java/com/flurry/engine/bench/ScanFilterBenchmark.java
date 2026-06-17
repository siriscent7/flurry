package com.flurry.engine.bench;

import com.flurry.engine.execution.ExecutionEngine;
import com.flurry.engine.execution.ParallelExecutor;
import com.flurry.engine.execution.Row;
import com.flurry.engine.parser.Parser;
import com.flurry.engine.parser.ast.Expr;
import com.flurry.engine.parser.ast.SelectStatement;
import com.flurry.engine.plan.LogicalPlan;
import com.flurry.engine.plan.LogicalPlanner;
import com.flurry.engine.storage.Catalog;
import com.flurry.engine.storage.CsvLoader;
import com.flurry.engine.storage.Table;
import org.openjdk.jmh.annotations.*;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
public class ScanFilterBenchmark {

    private Catalog catalog;
    private Table table;
    private LogicalPlan plan;
    private Expr predicate;
    private ExecutorService pool;

    private static final String SQL =
        "SELECT name, age, salary FROM big WHERE age >= 40 AND salary > 100000";

    @Setup(Level.Trial)
    public void setup() throws Exception {
        catalog = new Catalog();
        table = CsvLoader.load("big", Path.of("data/big.csv"));
        catalog.register(table);

        SelectStatement stmt = Parser.parse(SQL);
        plan = new LogicalPlanner(catalog).plan(stmt);
        predicate = stmt.where().orElse(null);

        pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @TearDown(Level.Trial)
    public void teardown() {
        pool.shutdown();
    }

    @Benchmark
    public int singleThreaded() {
        List<Row> rows = new ExecutionEngine(catalog).execute(plan);
        return rows.size();
    }

    @Benchmark
    public int parallel() throws Exception {
        List<Row> rows = new ParallelExecutor(table, predicate, pool, 50_000).run();
        return rows.size();
    }
}