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
 * Task 6: compare an append that fits in a deliberately sized ArrayList
 * with one that must grow its internal backing array.
 *
 * <p>The historical formula from the handout is printed as a prediction,
 * NOT as a claim about the capacity of a current Java implementation.</p>
 */
public final class ArrayListCapacityStudy {
    private static final int TRIALS = 160;
    private static volatile Object retainedList;

    public void run(Path destination) throws IOException {
        if (destination == null || destination.getParent() == null) {
            throw new IllegalArgumentException("Не задан допустимый путь отчёта");
        }
        Files.createDirectories(destination.getParent());
        warmUpJVM();
        try (BufferedWriter writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("Исследование времени ArrayList.add при увеличении ёмкости");
            writer.newLine();
            writer.write("Формула из пособия: (oldCapacity * 3) / 2 + 1");
            writer.newLine();
            writer.write("Проводятся " + TRIALS + " парных измерений для каждой начальной ёмкости.");
            writer.newLine();
            writer.write("Оба списка создаются как new ArrayList<>(oldCapacity).");
            writer.newLine();
            writer.write("Без роста: add при size = oldCapacity-1 (список ещё не заполнен).");
            writer.newLine();
            writer.write("С ростом: add при size = oldCapacity (добавляется oldCapacity+1-й элемент).");
            writer.newLine();
            writer.write("Следующий размер из старой формулы — расчётный, НЕ подтверждённый размер JVM.");
            writer.newLine();
            writer.write("oldCapacity;legacyNextCapacity;addWithoutGrowthNs;addWithGrowthNs;growthRatio");
            writer.newLine();
            for (int oldCapacity = 10, step = 0; step < 8; ++step) {
                int predicted = oldCapacity * 3 / 2 + 1;
                double inside = averageAppend(oldCapacity, false);
                double expanding = averageAppend(oldCapacity, true);
                writer.write(oldCapacity + ";" + predicted + ";" + decimal(inside) + ";"
                        + decimal(expanding) + ";" + decimal(expanding / Math.max(1, inside)));
                writer.newLine();
                oldCapacity = predicted;
            }
            writer.newLine();
            writer.write("Вывод: измерено изменение времени при заведомом требовании расширения "
                    + "явно заданной ёмкости. Реальную новую ёмкость ArrayList мы не считываем.");
            writer.newLine();
            writer.write("JIT/GC и разрешение System.nanoTime влияют на отдельные результаты. "
                    + "Повторение эксперимента рекомендуется.");
            writer.newLine();
        }
    }

    private static void warmUpJVM() {
        for (int i = 0; i < 5000; ++i) {
            ArrayList<Object> list = new ArrayList<Object>(10);
            for (int j = 0; j < 11; ++j) list.add(Boolean.TRUE);
            retainedList = list;
        }
    }

    /** Times only add(), not the constructor, initialization or volatile store. */
    private static double averageAppend(int capacity, boolean forceGrowth) {
        Object item = Boolean.TRUE;
        long total = 0;
        for (int trial = 0; trial < TRIALS; ++trial) {
            ArrayList<Object> list = new ArrayList<Object>(capacity);
            int initialCount = forceGrowth ? capacity : capacity - 1;
            for (int j = 0; j < initialCount; ++j) list.add(item);
            long start = System.nanoTime();
            list.add(item);
            total += System.nanoTime() - start;
            retainedList = list;  // Avoid dead-code elimination of the result.
        }
        return total / (double) TRIALS;
    }

    private static String decimal(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
