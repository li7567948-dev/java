package lab5.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Прочитанный файл: данные, исходный путь и собственные настройки просмотра. */
public final class DataFile {
    private final Path path;
    private final List<Measurement> measurements;
    private final ViewSettings settings;

    public DataFile(Path path, List<Measurement> rows, ViewSettings settings) {
        if (path == null || rows == null || rows.isEmpty() || settings == null) {
            throw new IllegalArgumentException("Не заданы данные эксперимента");
        }
        this.path = path.toAbsolutePath().normalize();
        this.measurements = Collections.unmodifiableList(new ArrayList<Measurement>(rows));
        this.settings = new ViewSettings(settings);
    }

    public Path getPath() { return path; }
    public String getName() { return path.getFileName().toString(); }
    public List<Measurement> getMeasurements() { return measurements; }
    public ViewSettings getSettings() { return new ViewSettings(settings); }
    @Override public String toString() { return getName(); }
}
