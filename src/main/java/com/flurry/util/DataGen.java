package com.flurry.engine.util;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/** Generates a large CSV for benchmarking. Usage: gen <rows> <outPath> */
public final class DataGen {

    private static final String[] CITIES =
        {"San Jose", "Seattle", "Austin", "Denver", "Boston", "Chicago", "Miami", "Portland"};
    private static final String[] NAMES =
        {"Alice", "Bob", "Carol", "Dave", "Eve", "Frank", "Grace", "Heidi"};

    public static void generate(int rows, Path out) throws Exception {
        Random rnd = new Random(42);
        try (BufferedWriter w = Files.newBufferedWriter(out)) {
            w.write("id,name,age,city,salary,active\n");
            for (int i = 1; i <= rows; i++) {
                String name = NAMES[rnd.nextInt(NAMES.length)];
                int age = 18 + rnd.nextInt(60);
                String city = CITIES[rnd.nextInt(CITIES.length)];
                int salary = 40000 + rnd.nextInt(160000);
                boolean active = rnd.nextBoolean();
                w.write(i + "," + name + "," + age + "," + city + "," + salary + "," + active + "\n");
            }
        }
        System.out.println("Wrote " + rows + " rows to " + out);
    }
}