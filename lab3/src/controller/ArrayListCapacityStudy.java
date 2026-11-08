package controller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Task 6: explore whether spikes occur around sizes predicted by the formula
 * oldCapacity*3/2+1. That formula describes historical implementations;
 * capacity is not exposed by the public ArrayList API in modern Java.
 */
public final class ArrayListCapacityStudy {
    private static final int TRIALS = 160;

    public void run(Path destination) throws IOException {
        if (destination == null) throw new IllegalArgumentException("Не задан путь отчёта");
        Files.createDirectories(destination.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("Исследование изменения времени ArrayList.add"); writer.newLine();
            writer.write("Формула из пособия: (oldCapacity * 3) / 2 + 1"); writer.newLine();
            writer.write("Для современных Java данная формула не является контрактом API.");
            writer.newLine();
            writer.write("Среднее время одной операции в нс по " + TRIALS + " запускам.");
            writer.newLine();
            writer.write("legacyCapacity;addBeforeNs;addAtBoundaryNs;addAfterNs"); writer.newLine();
            for (int oldCapacity = 10, step = 0; step < 8; ++step) {
                int candidate = oldCapacity * 3 / 2 + 1;
                double before = averageAddition(candidate - 2);
                double onBoundary = averageAddition(candidate - 1);
                double after = averageAddition(candidate);
                writer.write(candidate + ";" + format(before) + ";" + format(onBoundary)
                        + ";" + format(after));
                writer.newLine();
                oldCapacity = candidate;
            }
            writer.newLine();
            writer.write("Вывод: измеряются операции вокруг расчётных границ, "
                    + "но отдельный пик не доказывает внутреннюю фактическую ёмкость.");
            writer.newLine();
            writer.write("Для повторения используйте ту же JVM, увеличьте число измерений "
                    + "и учитывайте шум JIT/GC.");
            writer.newLine();
        }
    }

    /** Measures the addition at zero-based index insertionIndex. */
    private double averageAddition(int insertionIndex) {
        long total = 0;
        Object item = new Object();
        // Warm-up the addition path without measuring it.
        ArrayList<Object> warmup = new ArrayList<Object>();
        for (int i = 0; i < 10000; ++i) warmup.add(item);
        for (int trial = 0; trial < TRIALS; ++trial) {
            ArrayList<Object> list = new ArrayList<Object>();
            for (int i = 0; i < insertionIndex; ++i) list.add(item);
            long start = System.nanoTime();
            list.add(item);
            total += System.nanoTime() - start;
        }
        return total / (double) TRIALS;
    }

    private static String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
