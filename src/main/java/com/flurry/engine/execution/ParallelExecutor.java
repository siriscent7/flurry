package com.flurry.engine.execution;

import com.flurry.engine.parser.ast.Expr;
import com.flurry.engine.storage.Schema;
import com.flurry.engine.storage.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Morsel-driven parallel scan + filter.
 * Splits the table into row-range morsels, filters each morsel on a worker
 * thread, and merges the surviving rows. Demonstrates intra-operator parallelism.
 *
 * Two modes:
 *   - Owned pool:   pass a thread count; this class creates and shuts down its own pool.
 *   - Shared pool:  pass an existing ExecutorService (e.g. for benchmarking, so
 *                   thread-pool startup cost is not measured per query).
 */
public final class ParallelExecutor {

    private final Table table;
    private final Expr predicate;       // may be null (no filter)
    private final int morselSize;

    private final ExecutorService pool;
    private final boolean ownsPool;     // true if we created the pool and must shut it down

    /** Owned-pool constructor: creates its own fixed thread pool of `threads`. */
    public ParallelExecutor(Table table, Expr predicate, int threads, int morselSize) {
        this.table = table;
        this.predicate = predicate;
        this.morselSize = morselSize;
        this.pool = Executors.newFixedThreadPool(threads);
        this.ownsPool = true;
    }

    /** Shared-pool constructor: reuses an externally managed ExecutorService. */
    public ParallelExecutor(Table table, Expr predicate, ExecutorService pool, int morselSize) {
        this.table = table;
        this.predicate = predicate;
        this.morselSize = morselSize;
        this.pool = pool;
        this.ownsPool = false;
    }

    public List<Row> run() throws Exception {
        int total = table.rowCount();
        List<String> cols = new ArrayList<>();
        for (Schema.ColumnDef def : table.schema().columns()) cols.add(def.name());

        List<Future<List<Row>>> futures = new ArrayList<>();
        for (int start = 0; start < total; start += morselSize) {
            final int from = start;
            final int to = Math.min(start + morselSize, total);
            futures.add(pool.submit(() -> processMorsel(from, to, cols)));
        }

        List<Row> results = new ArrayList<>();
        try {
            for (Future<List<Row>> f : futures) {
                results.addAll(f.get());
            }
        } finally {
            if (ownsPool) {
                pool.shutdown();
            }
        }
        return results;
    }

    private List<Row> processMorsel(int from, int to, List<String> cols) {
        List<Row> local = new ArrayList<>();
        for (int r = from; r < to; r++) {
            Row row = new Row();
            for (int c = 0; c < cols.size(); c++) {
                row.put(cols.get(c), table.column(c).get(r));
            }
            if (predicate == null || ExpressionEvaluator.evalPredicate(predicate, row)) {
                local.add(row);
            }
        }
        return local;
    }
}