package com.flurry.engine.execution;

/**
 * Volcano-model physical operator.
 * Pull rows one at a time; next() returns null when exhausted.
 */
public interface Operator {
    void open();
    Row next();   // returns null when no more rows
    void close();
}