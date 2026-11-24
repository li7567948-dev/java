package lab5.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

/** Единая обработка ошибок с подсчётом и записью в отдельный файл. */
public final class ErrorHandler {
    private final Path logPath;
    private int errorCount;

    public ErrorHandler(Path logPath) { this.logPath = logPath; }

    public synchronized int getErrorCount() { return errorCount; }

    public synchronized String register(Exception exception) {
        errorCount++;
        String details = exception.getMessage();
        if (details == null || details.trim().isEmpty()) {
            details = exception.getClass().getSimpleName();
        }
        String message = "Ошибка №" + errorCount + ": " + details;
        try {
            Files.write(logPath, (LocalDateTime.now() + " | " + message + "\n")
                    .getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException writingError) {
            System.err.println("Не удалось сохранить журнал ошибок: " + writingError.getMessage());
        }
        return message;
    }
}
