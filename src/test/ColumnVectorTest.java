package com.flurry.engine.storage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColumnVectorTest {

    @Test
    void appendsAndComputesStats() {
        ColumnVector age = new ColumnVector("age", DataType.INT);
        age.appendRaw("30");
        age.appendRaw("25");
        age.appendRaw("");
        age.appendRaw("40");

        ColumnStats stats = age.stats();
        assertEquals(4, stats.rowCount());
        assertEquals(1, stats.nullCount());
        assertEquals(25, stats.min());
        assertEquals(40, stats.max());
    }

    @Test
    void inferenceWidensNumericTypes() {
        assertEquals(DataType.INT, DataType.infer("42"));
        assertEquals(DataType.DOUBLE, DataType.infer("3.14"));
        assertEquals(DataType.BOOLEAN, DataType.infer("true"));
        assertEquals(DataType.STRING, DataType.infer("hello"));
    }
}