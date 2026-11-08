package controller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

/**
 * Central message, operation-log and exception handler (lab No. 3, task 4).
 * All error messages and an error counter are kept in this one class.
 */
public final class ErrMsgLog {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss", Locale.ROOT);

    private final Path logDirectory;
    private int errorCount;

    public ErrMsgLog(Path logDirectory) throws IOException {
        if (logDirectory == null) throw new IllegalArgumentException("Папка логов не задана");
        Files.createDirectories(logDirectory);
        this.logDirectory = logDirectory;
    }

    public synchronized int getErrorCount() {
        return errorCount;
    }

    /** Centralized registration and formatting of all user-visible errors. */
    public synchronized String error(Exception exception) {
        ++errorCount;
        String explanation = exception.getMessage();
        if (explanation == null || explanation.trim().isEmpty()) {
            explanation = exception.getClass().getSimpleName();
        }
        String message = "Ошибка №" + errorCount + ": " + explanation;
        try (BufferedWriter writer = Files.newBufferedWriter(
                logDirectory.resolve("errors.log"), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(LocalDateTime.now().format(DATE) + " | " + message);
            writer.newLine();
        } catch (IOException logFailure) {
            // The handler still owns this fallback error message.
            System.err.println("Ошибка записи журнала: " + logFailure.getMessage());
        }
        return message;
    }

    public OperationLog openOperations(String kind, int size) throws IOException {
        if (!("ArrayList".equals(kind) || "HashMap".equals(kind))) {
            throw new IllegalArgumentException("Неизвестный тип коллекции");
        }
        if (size <= 0) throw new IllegalArgumentException("Размер коллекции должен быть положительным");
        return new OperationLog(logDirectory.resolve(kind + "_" + size + ".log"), kind, size);
    }

    /** Buffered log writer: I/O is explicitly outside timed operations. */
    public static final class OperationLog implements AutoCloseable {
        private final BufferedWriter writer;
        private boolean finished;

        private OperationLog(Path file, String kind, int size) throws IOException {
            writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            line("Start program: " + LocalDateTime.now().format(DATE));
            line(kind);
            line("Collection size = " + size);
            line("Time unit = nanoseconds (System.nanoTime)");
        }

        private void line(String text) throws IOException {
            writer.write(text);
            writer.newLine();
        }

        public void operation(String action, int key, long durationNanos) throws IOException {
            if (finished || durationNanos < 0 || key < 0) {
                throw new IllegalStateException("Некорректная запись операции");
            }
            line(action + ", ID = " + key + ", " + durationNanos);
        }

        public void finish(String insertionName, long[] insertTimes,
                           String replacementName, long[] replaceTimes) throws IOException {
            if (finished) throw new IllegalStateException("Журнал уже завершён");
            summarize(insertionName, insertTimes);
            summarize(replacementName, replaceTimes);
            long allTime = sum(insertTimes) + sum(replaceTimes);
            int count = insertTimes.length + replaceTimes.length;
            line("allOperationsTotalCount = " + count);
            line("allOperationsTotalTime = " + allTime);
            line("allOperationsAverageTime = " + decimal(allTime / (double) count));
            line("Finish program: " + LocalDateTime.now().format(DATE));
            finished = true;
            writer.flush();
        }

        private void summarize(String name, long[] times) throws IOException {
            if (times == null || times.length == 0) {
                throw new IllegalArgumentException("Операции не измерены");
            }
            long total = sum(times);
            long[] ordered = times.clone();
            Arrays.sort(ordered);
            double median = ordered.length % 2 == 0
                    ? (ordered[ordered.length / 2 - 1] / 2.0
                    + ordered[ordered.length / 2] / 2.0)
                    : ordered[ordered.length / 2];
            line(name + "TotalCount = " + times.length);
            line(name + "TotalTime = " + total);
            line(name + "AverageTime = " + decimal(total / (double) times.length));
            line(name + "MedianTime = " + decimal(median));
        }

        private static long sum(long[] times) {
            long total = 0;
            for (long value : times) total += value;
            return total;
        }

        private static String decimal(double value) {
            return String.format(Locale.US, "%.2f", value);
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }
    }
}
