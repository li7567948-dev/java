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

/** One place for all messages, errors and operation logs (tasks 3 and 4). */
public final class ErrMsgLog implements AutoCloseable {
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private final Path directory;
    private BufferedWriter writer;
    private int errorCounter;
    private int operationCounter;
    private long operationTotalTime;

    public ErrMsgLog(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
    }

    public int getErrorCount() { return errorCounter; }

    public Path getDirectory() { return directory; }

    /** Last-resort handling if initialization of the log directory itself fails. */
    public static void reportStartupError(Exception exception) {
        System.err.println("Ошибка запуска: " + exception.getMessage());
    }

    public void info(String message) { System.out.println(message); }

    /** Centralized creation of an analysis log, as required by task 4. */
    public void analysisLog(String filename, String content) throws IOException {
        Files.write(directory.resolve(filename), content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void error(Exception exception) {
        errorCounter++;
        String message = "Ошибка №" + errorCounter + ": " + exception.getMessage();
        System.err.println(message);
        try {
            Files.write(directory.resolve("errors.log"),
                    (LocalDateTime.now().format(TIME) + " " + message + "\n")
                            .getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException logError) {
            System.err.println("Не удалось записать ошибку в файл: " + logError.getMessage());
        }
    }

    public void openOperationLog(String collection, int size) throws IOException {
        finishOperationLog();
        Path path = directory.resolve(collection + "_" + size + ".log");
        writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
        operationCounter = 0;
        operationTotalTime = 0;
        write("Start program: " + LocalDateTime.now().format(TIME));
        write(collection);
        write("elements = " + size);
        write("timeUnit = ns");
    }

    public void operation(String type, int index, long timeNs) throws IOException {
        if (timeNs < 0) throw new IllegalArgumentException("Время операции отрицательное");
        write(type + ", ID = " + index + ", " + timeNs);
        operationCounter++;
        operationTotalTime += timeNs;
    }

    public void summary(String type, long[] times) throws IOException {
        long total = 0;
        for (long time : times) total += time;
        long[] ordered = times.clone();
        Arrays.sort(ordered);
        long median = 0;
        if (ordered.length != 0) {
            int mid = ordered.length / 2;
            median = ordered.length % 2 == 1 ? ordered[mid] :
                    ordered[mid - 1] / 2 + ordered[mid] / 2
                            + ((ordered[mid - 1] % 2 + ordered[mid] % 2) / 2);
        }
        write(type + "TotalCount = " + times.length);
        write(type + "TotalTime = " + total);
        write(type + "AverageTime = " + (times.length == 0 ? 0 : (double) total / times.length));
        // The assignment names the median field; report the actual statistical median.
        write(type + "MedianTime = " + median);
    }

    public void finishOperationLog() throws IOException {
        if (writer != null) {
            try {
                write("allOperationsTotalCount = " + operationCounter);
                write("allOperationsTotalTime = " + operationTotalTime);
                write("allOperationsAverageTime = "
                        + (operationCounter == 0 ? 0 : (double) operationTotalTime / operationCounter));
                write("Finish program: " + LocalDateTime.now().format(TIME));
            } finally {
                try { writer.close(); }
                finally { writer = null; }
            }
        }
    }

    private void write(String text) throws IOException {
        if (writer == null) throw new IllegalStateException("Лог не открыт");
        writer.write(text);
        writer.newLine();
    }

    @Override public void close() throws IOException { finishOperationLog(); }
}
