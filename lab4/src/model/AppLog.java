package model;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Журнал с метками времени; открывается в режиме добавления. */
public final class AppLog implements AutoCloseable {
    private final BufferedWriter writer;

    private boolean debug;

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public AppLog(Path path) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path.toFile(), true),
                StandardCharsets.UTF_8));
    }

    public void setDebug(boolean enabled) {
        debug = enabled;
    }

    public boolean isDebug() {
        return debug;
    }

    public void info(String message) throws IOException {
        write("INFO", message);
    }

    public void error(String message) throws IOException {
        write("ERROR", message);
    }

    public void test(String message) throws IOException {
        write("TEST", message);
    }

    public void debug(String message) throws IOException {
        if (debug) {
            write("DEBUG", message);
        }
    }

    /** Записывает одну строку журнала и немедленно сбрасывает буфер. */
    private void write(String level, String message) throws IOException {
        writer.write(LocalDateTime.now().format(TIMESTAMP_FORMAT) + " [" + level + "] " + message.replace('\r',
                ' ').replace('\n', ' '));
        writer.newLine();
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
