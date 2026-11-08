package controller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import model.Lab3ObjectGenerator;
import model.Simulation;

/**
 * Measures exactly the operations required by laboratory work No. 3.
 * Five N values × two collection classes = ten UTF-8 log files.
 *
 * <p>Object generation, index selection, log writing and summary calculations
 * take place OUTSIDE each measured nanoTime interval.</p>
 */
public final class BenchmarkService {
    public static final int[] SIZES = {10, 100, 1000, 10000, 100000};

    private final ErrMsgLog messages;
    private final Path resultDirectory;

    public BenchmarkService(ErrMsgLog messages, Path resultDirectory) {
        if (messages == null || resultDirectory == null) {
            throw new IllegalArgumentException("Не заданы журналы эксперимента");
        }
        this.messages = messages;
        this.resultDirectory = resultDirectory;
    }

    /** Returns the ten result rows, also stored in results/summary.csv. */
    public List<Measurement> runAll() throws IOException {
        Files.createDirectories(resultDirectory);
        List<Measurement> results = new ArrayList<Measurement>();
        for (int size : SIZES) {
            results.add(runArrayList(size));
            results.add(runHashMap(size));
        }
        writeSummary(results);
        return results;
    }

    public Measurement runArrayList(int size) throws IOException {
        requireSize(size);
        Lab3ObjectGenerator generator = new Lab3ObjectGenerator(7000L + size);
        ArrayList<Simulation> simulations = new ArrayList<Simulation>();
        long[] additions = new long[size];
        long[] replacements = new long[size / 10];
        int[] targets = randomUniqueIndices(size, size / 10, 21000L + size);

        try (ErrMsgLog.OperationLog log = messages.openOperations("ArrayList", size)) {
            for (int id = 0; id < size; ++id) {
                Simulation object = generator.generateSimulation(id);
                long start = System.nanoTime();
                simulations.add(object);
                long elapsed = System.nanoTime() - start;
                additions[id] = elapsed;
                log.operation("add", id, elapsed);
            }
            for (int i = 0; i < targets.length; ++i) {
                int index = targets[i];
                Simulation object = generator.generateSimulation(size + i);
                long start = System.nanoTime();
                simulations.set(index, object);
                long elapsed = System.nanoTime() - start;
                replacements[i] = elapsed;
                log.operation("set", index, elapsed);
            }
            log.finish("add", additions, "set", replacements);
        }
        if (simulations.size() != size) throw new IllegalStateException("Размер ArrayList изменился");
        return new Measurement("ArrayList", size, sum(additions), sum(replacements));
    }

    public Measurement runHashMap(int size) throws IOException {
        requireSize(size);
        Lab3ObjectGenerator generator = new Lab3ObjectGenerator(7000L + size);
        HashMap<Integer, Simulation> simulations = new HashMap<Integer, Simulation>();
        long[] additions = new long[size];
        long[] replacements = new long[size / 10];
        int[] targets = randomUniqueIndices(size, size / 10, 21000L + size);

        try (ErrMsgLog.OperationLog log = messages.openOperations("HashMap", size)) {
            for (int id = 0; id < size; ++id) {
                Simulation object = generator.generateSimulation(id);
                long start = System.nanoTime();
                simulations.put(id, object);
                long elapsed = System.nanoTime() - start;
                additions[id] = elapsed;
                log.operation("put", id, elapsed);
            }
            for (int i = 0; i < targets.length; ++i) {
                int key = targets[i];
                Simulation object = generator.generateSimulation(size + i);
                long start = System.nanoTime();
                Simulation replaced = simulations.replace(key, object);
                long elapsed = System.nanoTime() - start;
                if (replaced == null) throw new IllegalStateException("Ключ отсутствует: " + key);
                replacements[i] = elapsed;
                log.operation("replace", key, elapsed);
            }
            log.finish("put", additions, "replace", replacements);
        }
        if (simulations.size() != size) throw new IllegalStateException("Размер HashMap изменился");
        return new Measurement("HashMap", size, sum(additions), sum(replacements));
    }

    private void requireSize(int size) {
        if (size <= 0 || size % 10 != 0) {
            throw new IllegalArgumentException("Размер должен быть положительным и кратным 10");
        }
    }

    /** Partial Fisher-Yates shuffle: no repeated random modification index. */
    public static int[] randomUniqueIndices(int size, int count, long seed) {
        if (size < 0 || count < 0 || count > size) {
            throw new IllegalArgumentException("Неверное количество случайных индексов");
        }
        int[] all = new int[size];
        for (int id = 0; id < size; ++id) all[id] = id;
        Random random = new Random(seed);
        for (int index = 0; index < count; ++index) {
            int next = index + random.nextInt(size - index);
            int temporary = all[index];
            all[index] = all[next];
            all[next] = temporary;
        }
        int[] selected = new int[count];
        System.arraycopy(all, 0, selected, 0, count);
        return selected;
    }

    private void writeSummary(List<Measurement> measurements) throws IOException {
        Path csv = resultDirectory.resolve("summary.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(csv, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("collection;size;addTotalNs;addAverageNs;changeTotalNs;changeAverageNs");
            writer.newLine();
            for (Measurement row : measurements) {
                writer.write(row.kind + ";" + row.size + ";" + row.addTotal + ";"
                        + format(row.getAddAverage()) + ";" + row.changeTotal + ";"
                        + format(row.getChangeAverage()));
                writer.newLine();
            }
        }
        writeConclusion(measurements);
    }

    private void writeConclusion(List<Measurement> results) throws IOException {
        Path conclusion = resultDirectory.resolve("conclusion_ru.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(conclusion, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("ВЫВОД ПО ЛАБОРАТОРНОЙ РАБОТЕ № 3"); writer.newLine();
            writer.write("Данные являются результатами конкретного запуска JVM; единица: нс.");
            writer.newLine();
            writer.write("Генерация объектов и запись в лог не включены в измеряемую операцию.");
            writer.newLine();
            for (String kind : new String[] {"ArrayList", "HashMap"}) {
                Measurement first = lookup(results, kind, 10);
                Measurement last = lookup(results, kind, 100000);
                writer.write(kind + ": при увеличении N с 10 до 100000 ");
                writer.write("среднее время добавления изменилось с " + format(first.getAddAverage())
                        + " до " + format(last.getAddAverage()) + " нс; ");
                writer.write("среднее время изменения с " + format(first.getChangeAverage())
                        + " до " + format(last.getChangeAverage()) + " нс.");
                writer.newLine();
                writer.write("Отношение полных времён добавления (100000/10): "
                        + format(last.addTotal / (double) Math.max(1, first.addTotal)));
                writer.newLine();
                writer.write("Отношение полных времён изменения (100000/10): "
                        + format(last.changeTotal / (double) Math.max(1, first.changeTotal)));
                writer.newLine();
            }
            writer.write("Сравнение: доступ к элементу ArrayList по индексу и HashMap по ключу "
                    + "обычно быстрый; увеличение ёмкости может создавать отдельные пики задержек.");
            writer.newLine();
            writer.write("При малых N измерения шумные из-за JIT, сборщика мусора и таймера; "
                    + "для устойчивых выводов необходимо повторение экспериментов.");
            writer.newLine();
            writer.write("Среднее арифметическое (Average) и медиана (Median) "
                    + "вычислены отдельно и не подменяют друг друга.");
            writer.newLine();
        }
    }

    private static Measurement lookup(List<Measurement> measurements, String type, int size) {
        for (Measurement item : measurements) {
            if (item.kind.equals(type) && item.size == size) return item;
        }
        throw new IllegalStateException("Нет результата " + type + " N=" + size);
    }

    private static String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static long sum(long[] values) {
        long total = 0;
        for (long value : values) total += value;
        return total;
    }

    public static final class Measurement {
        private final String kind;
        private final int size;
        private final long addTotal;
        private final long changeTotal;

        private Measurement(String kind, int size, long addTotal, long changeTotal) {
            this.kind = kind;
            this.size = size;
            this.addTotal = addTotal;
            this.changeTotal = changeTotal;
        }

        public String getKind() { return kind; }
        public int getSize() { return size; }
        public long getAddTotal() { return addTotal; }
        public long getChangeTotal() { return changeTotal; }
        public double getAddAverage() { return addTotal / (double) size; }
        public double getChangeAverage() { return changeTotal / (double) (size / 10); }
    }
}
