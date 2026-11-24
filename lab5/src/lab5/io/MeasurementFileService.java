package lab5.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lab5.model.DataFile;
import lab5.model.Measurement;
import lab5.model.ViewSettings;

/** Чтение сводной CSV ЛР3, исходных логов ЛР3 и автономных наборов ЛР5. */
public final class MeasurementFileService {
    private static final String VERSION = "# LAB5_DATA_V1";
    private static final String CSV_HEADER = "collection;size;addTotalNs;addAverageNs;addMedianNs;"
            + "changeTotalNs;changeAverageNs;changeMedianNs";
    private static final String BUNDLE_HEADER = "source;" + CSV_HEADER;
    private static final Pattern OPERATION = Pattern.compile(
            "^(add|put|set|replace|remove),\\s*ID\\s*=\\s*(\\d+),\\s*(\\d+(?:\\.\\d+)?)\\s*$");
    private static final Pattern SUMMARY = Pattern.compile(
            "^([a-z]+)(TotalCount|TotalTime|AverageTime|MedianTime)\\s*=\\s*(\\d+(?:\\.\\d+)?)\\s*$");
    private static final Pattern SIZE = Pattern.compile("^Collection size\\s*=\\s*(\\d+)\\s*$");

    public DataFile read(Path path) throws IOException {
        if (path == null) throw new IllegalArgumentException("Не указан файл");
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized)) {
            throw new IOException("Файл не найден: " + normalized);
        }
        String name = normalized.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".csv")) return readDelimited(normalized, false);
        if (name.endsWith(".lab5")) return readDelimited(normalized, true);
        if (name.endsWith(".log")) return readLog(normalized);
        throw new IOException("Поддерживаются файлы .csv, .log и .lab5");
    }

    private DataFile readDelimited(Path path, boolean bundled) throws IOException {
        List<Measurement> rows = new ArrayList<Measurement>();
        ViewSettings settings = new ViewSettings();
        Set<String> unique = new HashSet<String>();
        boolean hasVersion = !bundled;
        boolean hasHeader = false;
        int lineNumber = 0;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                ++lineNumber;
                if (lineNumber == 1 && line.startsWith("\uFEFF")) line = line.substring(1);
                if (line.trim().isEmpty()) continue;
                if (line.equals(VERSION) && bundled) {
                    hasVersion = true;
                    continue;
                }
                if (line.startsWith("#")) {
                    if (bundled && line.startsWith("# view.")) {
                        applySettingLine(settings, line.substring(2), path, lineNumber);
                    }
                    continue;
                }
                if (!hasHeader) {
                    String required = bundled ? BUNDLE_HEADER : CSV_HEADER;
                    if (!line.equals(required)) {
                        throw invalid(path, lineNumber, "Неверные имена колонок CSV");
                    }
                    hasHeader = true;
                    continue;
                }
                List<String> cells = csvFields(line);
                int expected = bundled ? 9 : 8;
                if (cells.size() != expected) {
                    throw invalid(path, lineNumber, "Ожидалось " + expected + " столбцов, получено " + cells.size());
                }
                int shift = bundled ? 1 : 0;
                String source = bundled ? cells.get(0) : "";
                String collection = cells.get(shift);
                int size = parsePositiveInt(cells.get(shift + 1), path, lineNumber);
                try {
                    Measurement row = new Measurement(source, collection, size,
                            parseNumber(cells.get(shift + 2)),
                            parseNumber(cells.get(shift + 3)),
                            optionalNumber(cells.get(shift + 4)),
                            parseNumber(cells.get(shift + 5)),
                            parseNumber(cells.get(shift + 6)),
                            optionalNumber(cells.get(shift + 7)));
                    String key = row.getSourceTag() + "\u0001" + row.getCollection() + "\u0001" + size;
                    verifyCsvMetrics(row, path, lineNumber);
                    if (!unique.add(key)) {
                        throw invalid(path, lineNumber, "Повтор записи коллекции для N=" + size);
                    }
                    rows.add(row);
                } catch (IllegalArgumentException exception) {
                    throw invalid(path, lineNumber, exception.getMessage());
                }
            }
        }
        if (!hasVersion || !hasHeader || rows.isEmpty()) {
            throw new IOException("Файл не содержит правильных данных: " + path.getFileName());
        }
        if (!bundled) settings = readSidecar(path);
        return new DataFile(path, rows, settings);
    }

    private DataFile readLog(Path path) throws IOException {
        String collection = null;
        int announcedSize = 0;
        int addCount = 0;
        int changeCount = 0;
        double addTotal = 0;
        double changeTotal = 0;
        String additionAction = null;
        String changeAction = null;
        double factor = 1.0;
        Map<String, Double> summary = new HashMap<String, Double>();
        int lineNumber = 0;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                ++lineNumber;
                if (lineNumber == 1 && line.startsWith("\uFEFF")) line = line.substring(1);
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.equals("ArrayList") || line.equals("HashMap") || line.equals("LinkedList")) {
                    collection = line;
                    continue;
                }
                Matcher sizes = SIZE.matcher(line);
                if (sizes.matches()) {
                    announcedSize = parsePositiveInt(sizes.group(1), path, lineNumber);
                    continue;
                }
                if (line.startsWith("Time unit = ")) {
                    if (addCount != 0 || changeCount != 0) {
                        throw invalid(path, lineNumber, "Единица времени задана после операций");
                    }
                    if (line.contains("nanoseconds")) factor = 1.0;
                    else if (line.contains("milliseconds")) factor = 1000000.0;
                    else throw invalid(path, lineNumber, "Неизвестная единица времени");
                    continue;
                }
                Matcher operation = OPERATION.matcher(line);
                if (operation.matches()) {
                    String action = operation.group(1);
                    int key = parseNonnegativeInt(operation.group(2), path, lineNumber);
                    if (announcedSize > 0 && key >= announcedSize) {
                        throw invalid(path, lineNumber, "Индекс операции вне диапазона коллекции");
                    }
                    double time;
                    try { time = parseNumber(operation.group(3)) * factor; }
                    catch (IllegalArgumentException exception) {
                        throw invalid(path, lineNumber, exception.getMessage());
                    }
                    if (Double.isNaN(time) || Double.isInfinite(time) || time < 0) {
                        throw invalid(path, lineNumber, "Неверное время операции");
                    }
                    if (action.equals("add") || action.equals("put")) {
                        if (additionAction != null && !additionAction.equals(action)) {
                            throw invalid(path, lineNumber, "Смешаны add и put");
                        }
                        additionAction = action;
                        addTotal += time;
                        ++addCount;
                    } else {
                        if (changeAction != null && !changeAction.equals(action)) {
                            throw invalid(path, lineNumber, "Смешаны различные операции изменения");
                        }
                        changeAction = action;
                        changeTotal += time;
                        ++changeCount;
                    }
                    continue;
                }
                if (line.matches("^(add|put|set|replace|remove)\\s*,.*")) {
                    throw invalid(path, lineNumber, "Повреждённая запись операции");
                }
                Matcher result = SUMMARY.matcher(line);
                if (result.matches()) {
                    summary.put(result.group(1) + result.group(2),
                            parseNumber(result.group(3)) * (result.group(2).contains("Time") ? factor : 1.0));
                }
            }
        }
        if (collection == null || addCount == 0 || changeCount == 0 || announcedSize != addCount) {
            throw new IOException("Лог не полон или не соответствует формату ЛР3: " + path.getFileName());
        }
        if (announcedSize % 10 != 0 || changeCount != announcedSize / 10) {
            throw new IOException("В ЛР3 требуется изменить ровно 10% элементов: " + path.getFileName());
        }
        if (collection.equals("HashMap") && (!"put".equals(additionAction)
                || !("replace".equals(changeAction) || "remove".equals(changeAction)))) {
            throw new IOException("Операции HashMap не соответствуют ЛР3");
        }
        if (!collection.equals("HashMap") && (!"add".equals(additionAction)
                || !("set".equals(changeAction) || "remove".equals(changeAction)))) {
            throw new IOException("Операции списка не соответствуют ЛР3");
        }
        verifySummary(summary, additionAction, addCount, addTotal, path);
        verifySummary(summary, changeAction, changeCount, changeTotal, path);
        Double globalCount = summary.get("allOperationsTotalCount");
        Double globalTime = summary.get("allOperationsTotalTime");
        if ((globalCount != null && Math.abs(globalCount - addCount - changeCount) > 1e-6)
                || (globalTime != null && Math.abs(globalTime - addTotal - changeTotal) > 0.01)) {
            throw new IOException("Итог всех операций не совпадает с логом: " + path.getFileName());
        }
        Double addMedian = summary.get(additionAction + "MedianTime");
        Double changeMedian = summary.get(changeAction + "MedianTime");
        List<Measurement> result = Collections.singletonList(new Measurement("", collection,
                addCount, addTotal, addTotal / addCount, addMedian,
                changeTotal, changeTotal / changeCount, changeMedian));
        return new DataFile(path, result, readSidecar(path));
    }

    private static void verifyCsvMetrics(Measurement row, Path file, int line) throws IOException {
        int size = row.getSize();
        if (size % 10 != 0) throw invalid(file, line, "Размер ЛР3 должен быть кратен десяти");
        double addExpected = row.getAddTotalNs() / size;
        double changeExpected = row.getChangeTotalNs() / (size / 10.0);
        if (Math.abs(row.getAddAverageNs() - addExpected) > 0.011
                || Math.abs(row.getChangeAverageNs() - changeExpected) > 0.011) {
            throw invalid(file, line, "Среднее время не согласуется с общим временем и числом операций");
        }
    }

    private static void verifySummary(Map<String, Double> summary, String name,
            int count, double total, Path path) throws IOException {
        Double statedCount = summary.get(name + "TotalCount");
        Double statedTotal = summary.get(name + "TotalTime");
        Double statedAverage = summary.get(name + "AverageTime");
        if (statedCount != null && Math.abs(statedCount - count) > 1e-6) {
            throw new IOException("Счётчик операций не совпадает с логом: " + path.getFileName());
        }
        if (statedTotal != null && Math.abs(statedTotal - total) > Math.max(1e-5, total * 1e-9)) {
            throw new IOException("Суммарное время не совпадает с логом: " + path.getFileName());
        }
        if (statedAverage != null && Math.abs(statedAverage - total / count) > 0.011) {
            throw new IOException("Среднее время не совпадает с логом: " + path.getFileName());
        }
    }

    public void saveSettings(Path original, ViewSettings settings) throws IOException {
        if (original == null || settings == null) throw new IllegalArgumentException("Нет настроек");
        if (original.toString().toLowerCase(Locale.ROOT).endsWith(".lab5")) {
            DataFile existing = read(original);
            saveBundle(original, Collections.singletonList(existing), settings);
            return;
        }
        Path sidecar = sidecarPath(original);
        Properties properties = new Properties();
        storeSettings(properties, settings);
        Path temporary = temporaryPath(sidecar);
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                properties.store(writer, "Java Lab 5 settings (original data are unchanged)");
            }
            moveIntoPlace(temporary, sidecar);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Формат .lab5 хранит строки измерений И параметры графиков в ОДНОМ файле. */
    public void saveBundle(Path path, List<DataFile> files, ViewSettings settings) throws IOException {
        if (files == null || files.isEmpty()) throw new IllegalArgumentException("Нет данных для сохранения");
        if (settings == null) throw new IllegalArgumentException("Нет настроек отображения");
        Path target = path.toAbsolutePath().normalize();
        Path temporary = temporaryPath(target);
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                writer.write(VERSION); writer.newLine();
                writer.write("# view.zoom=" + String.format(Locale.US, "%.2f", settings.getZoom())); writer.newLine();
                writer.write("# view.legend=" + settings.isLegendVisible()); writer.newLine();
                writer.write("# view.xScale=" + settings.getXScale().name()); writer.newLine();
                writer.write("# view.yScale=" + settings.getYScale().name()); writer.newLine();
                writer.write(BUNDLE_HEADER); writer.newLine();
                for (DataFile dataFile : files) {
                    for (Measurement row : dataFile.getMeasurements()) {
                        // Повторное сохранение .lab5 не должно добавлять его имя к исходному тегу.
                        boolean alreadyBundled = dataFile.getName().toLowerCase(Locale.ROOT).endsWith(".lab5");
                        String tag = row.getSourceTag().isEmpty() ? dataFile.getName()
                                : alreadyBundled ? row.getSourceTag()
                                : dataFile.getName() + " / " + row.getSourceTag();
                        writer.write(encodeCell(tag) + ";" + encodeCell(row.getCollection()) + ";" + row.getSize()
                                + ";" + number(row.getAddTotalNs())
                                + ";" + number(row.getAddAverageNs())
                                + ";" + optional(row.getAddMedianNs())
                                + ";" + number(row.getChangeTotalNs())
                                + ";" + number(row.getChangeAverageNs())
                                + ";" + optional(row.getChangeMedianNs()));
                        writer.newLine();
                    }
                }
            }
            moveIntoPlace(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String number(double value) { return Double.toString(value); }
    private static String optional(Double value) { return value == null ? "" : number(value); }
    private static String encodeCell(String value) {
        if (value.indexOf(';') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0) return value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static ViewSettings readSidecar(Path original) throws IOException {
        ViewSettings settings = new ViewSettings();
        Path sidecar = sidecarPath(original);
        if (!Files.isRegularFile(sidecar)) return settings;
        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(sidecar, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        try {
            settings.setZoom(Double.parseDouble(properties.getProperty("view.zoom", "1")));
            String legendText = properties.getProperty("view.legend", "true");
            if (!legendText.equals("true") && !legendText.equals("false")) {
                throw new IllegalArgumentException("Неверный флаг легенды");
            }
            settings.setLegendVisible(Boolean.parseBoolean(legendText));
            settings.setXScale(ViewSettings.Scale.valueOf(properties.getProperty("view.xScale", "LOGARITHMIC")));
            settings.setYScale(ViewSettings.Scale.valueOf(properties.getProperty("view.yScale", "LINEAR")));
        } catch (IllegalArgumentException exception) {
            throw new IOException("Повреждены сохранённые настройки: " + sidecar.getFileName(), exception);
        }
        return settings;
    }

    private static void storeSettings(Properties properties, ViewSettings settings) {
        properties.setProperty("view.zoom", Double.toString(settings.getZoom()));
        properties.setProperty("view.legend", Boolean.toString(settings.isLegendVisible()));
        properties.setProperty("view.xScale", settings.getXScale().name());
        properties.setProperty("view.yScale", settings.getYScale().name());
    }

    private static void applySettingLine(ViewSettings settings, String property,
            Path file, int lineNumber) throws IOException {
        int divider = property.indexOf('=');
        if (divider < 1) throw invalid(file, lineNumber, "Неверный параметр графика");
        String key = property.substring(0, divider);
        String value = property.substring(divider + 1);
        try {
            if (key.equals("view.zoom")) settings.setZoom(Double.parseDouble(value));
            else if (key.equals("view.legend")) {
                if (!value.equals("true") && !value.equals("false")) throw new IllegalArgumentException("Некорректный флаг легенды");
                settings.setLegendVisible(Boolean.parseBoolean(value));
            } else if (key.equals("view.xScale")) settings.setXScale(ViewSettings.Scale.valueOf(value));
            else if (key.equals("view.yScale")) settings.setYScale(ViewSettings.Scale.valueOf(value));
        } catch (IllegalArgumentException exception) {
            throw invalid(file, lineNumber, "Неверная настройка: " + property);
        }
    }

    private static Path sidecarPath(Path original) {
        Path path = original.toAbsolutePath().normalize();
        return Paths.get(path.toString() + ".view.properties");
    }
    private static Path temporaryPath(Path target) {
        return target.resolveSibling(target.getFileName().toString() + ".tmp");
    }
    private static void moveIntoPlace(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static List<String> csvFields(String line) throws IOException {
        List<String> fields = new ArrayList<String>();
        StringBuilder item = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    item.append('"'); ++i;
                } else quoted = !quoted;
            } else if (c == ';' && !quoted) {
                fields.add(item.toString().trim()); item.setLength(0);
            } else item.append(c);
        }
        if (quoted) throw new IOException("Незакрытые кавычки в CSV");
        fields.add(item.toString().trim());
        return fields;
    }

    private static double parseNumber(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Пропущено число");
        double number;
        try { number = Double.parseDouble(value.trim()); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Неверное число: " + value); }
        if (number < 0 || Double.isNaN(number) || Double.isInfinite(number)) {
            throw new IllegalArgumentException("Недопустимое значение: " + value);
        }
        return number;
    }
    private static Double optionalNumber(String value) { return value.isEmpty() ? null : parseNumber(value); }

    private static int parseNonnegativeInt(String value, Path path, int line) throws IOException {
        try {
            int result = Integer.parseInt(value);
            if (result < 0) throw new NumberFormatException();
            return result;
        } catch (NumberFormatException exception) {
            throw invalid(path, line, "Неверный идентификатор или размер");
        }
    }
    private static int parsePositiveInt(String value, Path path, int line) throws IOException {
        int result = parseNonnegativeInt(value, path, line);
        if (result == 0) throw invalid(path, line, "Число элементов должно быть положительным");
        return result;
    }
    private static IOException invalid(Path path, int line, String detail) {
        return new IOException(path.getFileName() + ", строка " + line + ": " + detail);
    }
}
