package lab5.tests;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

import lab5.io.MeasurementFileService;
import lab5.model.DataFile;
import lab5.model.Measurement;
import lab5.model.ViewSettings;
import lab5.view.GraphPanel;

/** Автоматические регрессионные тесты без сторонних библиотек. */
public final class Lab5Tests {
    private static int passed;
    private Lab5Tests() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        MeasurementFileService io = new MeasurementFileService();
        Path csv = Paths.get("data", "lab3_summary.csv");
        if (!Files.isRegularFile(csv)) csv = Paths.get("lab5", "data", "lab3_summary.csv");
        DataFile original = io.read(csv);
        Path builtInBundle = csv.resolveSibling("lab3_ready.lab5");
        DataFile deliveredBundle = io.read(builtInBundle);
        check(deliveredBundle.getMeasurements().size() == 10, "Прилагаемый файл .lab5 читается без ошибок");
        check(deliveredBundle.getMeasurements().get(0).getAddTotalNs() == 1359,
                "Прилагаемый .lab5 не подменяет реальные данные");
        check(deliveredBundle.getSettings().getXScale() == ViewSettings.Scale.LOGARITHMIC,
                "Встроенная настройка .lab5 корректна");
        check(original.getMeasurements().size() == 10, "В CSV десять строк");
        check(original.getMeasurements().get(0).getCollection().equals("ArrayList"), "Тип первой коллекции");
        check(original.getMeasurements().get(1).getCollection().equals("HashMap"), "Тип второй коллекции");
        int[] sizes = {10, 100, 1000, 10000, 100000};
        for (int n : sizes) {
            check(find(original, "ArrayList", n) != null, "ArrayList/" + n);
            check(find(original, "HashMap", n) != null, "HashMap/" + n);
        }
        Measurement last = find(original, "HashMap", 100000);
        check(last.getAddTotalNs() == 19668274, "Исходное время HashMap, N=100000");
        check(last.getChangeTotalNs() == 3990433, "Исходное время replace, N=100000");
        check(last.getAddMedianNs() == 53, "Медиана ЛР3 сохранена");
        check(find(original, "ArrayList", 100).getChangeAverageNs() == 549.80,
                "Среднее изменение ArrayList, N=100");
        check(original.getSettings().getXScale() == ViewSettings.Scale.LOGARITHMIC,
                "Логарифмическая ось X по умолчанию");

        Path temporary = Files.createTempDirectory("lab5-tests-");
        try {
            ViewSettings view = new ViewSettings();
            view.setZoom(1.75);
            view.setLegendVisible(false);
            view.setXScale(ViewSettings.Scale.LINEAR);
            view.setYScale(ViewSettings.Scale.LOGARITHMIC);
            Path bundle = temporary.resolve("lab5_data.lab5");
            io.saveBundle(bundle, Collections.singletonList(original), view);
            DataFile reopened = io.read(bundle);
            check(reopened.getMeasurements().size() == 10, "Сохранение всех измерений");
            check(reopened.getSettings().getZoom() == 1.75, "Восстановление масштаба");
            check(!reopened.getSettings().isLegendVisible(), "Восстановление легенды");
            check(reopened.getSettings().getXScale() == ViewSettings.Scale.LINEAR, "Восстановление оси X");
            check(reopened.getSettings().getYScale() == ViewSettings.Scale.LOGARITHMIC, "Восстановление оси Y");
            check(reopened.getMeasurements().get(0).getAddMedianNs() == 134.50,
                    "Медиана сохранена без искажения");
            String originalTag = reopened.getMeasurements().get(0).getSourceTag();
            io.saveSettings(bundle, view);
            check(io.read(bundle).getMeasurements().get(0).getSourceTag().equals(originalTag),
                    "При повторном сохранении источники не переименовываются");

            Path copy = temporary.resolve("experiment.csv");
            Files.copy(csv, copy);
            io.saveSettings(copy, view);
            check(Files.exists(temporary.resolve("experiment.csv.view.properties")),
                    "Настройки для CSV сохраняются рядом с исходными данными");
            check(io.read(copy).getSettings().getZoom() == 1.75,
                    "Настройки CSV загружаются повторно");
            check(io.read(csv).getSettings().getZoom() == 1.0,
                    "Оригинальный CSV не изменён");

            Path log = temporary.resolve("ArrayList_10.log");
            StringBuilder text = new StringBuilder();
            text.append("Start program: 01.11.2026 10:00:00\n")
                .append("ArrayList\nCollection size = 10\n")
                .append("Time unit = nanoseconds (System.nanoTime)\n");
            for (int i = 0; i < 10; i++) {
                text.append("add, ID = ").append(i).append(", ").append((i + 1) * 10).append('\n');
            }
            text.append("addTotalCount = 10\naddTotalTime = 550\n")
                .append("addAverageTime = 55.00\naddMedianTime = 55.00\n")
                .append("set, ID = 3, 100\nsetTotalCount = 1\n")
                .append("setTotalTime = 100\nsetAverageTime = 100.00\n")
                .append("setMedianTime = 100.00\n")
                .append("allOperationsTotalCount = 11\n")
                .append("allOperationsTotalTime = 650\n")
                .append("allOperationsAverageTime = 59.09\nFinish program: 01.11.2026 10:00:01\n");
            Files.write(log, text.toString().getBytes(StandardCharsets.UTF_8));
            DataFile parsedLog = io.read(log);
            check(parsedLog.getMeasurements().size() == 1, "Лог содержит одно значение N");
            check(parsedLog.getMeasurements().get(0).getSize() == 10, "Размер получен из лога");
            check(parsedLog.getMeasurements().get(0).getAddTotalNs() == 550, "Сумма из строк add");
            check(parsedLog.getMeasurements().get(0).getChangeTotalNs() == 100, "Сумма из строк set");
            check(parsedLog.getMeasurements().get(0).getChangeAverageNs() == 100, "Среднее из лога");
            check(parsedLog.getMeasurements().get(0).getAddMedianNs() == 55, "Медиана из лога");
            io.saveSettings(log, view);
            check(io.read(log).getSettings().getYScale() == ViewSettings.Scale.LOGARITHMIC,
                    "Настройки LOG восстанавливаются");

            Path corruptLog = temporary.resolve("corrupt.log");
            Files.write(corruptLog, text.toString().replace("addTotalTime = 550", "addTotalTime = 599")
                    .getBytes(StandardCharsets.UTF_8));
            expectIOException(() -> io.read(corruptLog), "Повреждённая сумма лог-файла обнаруживается");
            Path brokenCsv = temporary.resolve("broken.csv");
            Files.write(brokenCsv, (new String(Files.readAllBytes(csv), StandardCharsets.UTF_8)
                    + "ArrayList;10;1;1;1;1;1;1\n").getBytes(StandardCharsets.UTF_8));
            expectIOException(() -> io.read(brokenCsv), "Дубликат размера и коллекции запрещён");
            Path negativeCsv = temporary.resolve("negative.csv");
            Files.write(negativeCsv, (new String(Files.readAllBytes(csv), StandardCharsets.UTF_8)
                    .replace("ArrayList;10;1359", "ArrayList;10;-1359"))
                    .getBytes(StandardCharsets.UTF_8));
            expectIOException(() -> io.read(negativeCsv), "Отрицательное время запрещено");
            Path invalidExt = temporary.resolve("source.txt");
            Files.write(invalidExt, "data".getBytes(StandardCharsets.UTF_8));
            expectIOException(() -> io.read(invalidExt), "Неподдерживаемый тип файла отклонён");

            // Десять LOG-файлов должны соединяться в две линии (ArrayList / HashMap).
            List<DataFile> tenFiles = new ArrayList<DataFile>();
            for (Measurement row : original.getMeasurements()) {
                Path pretendLog = temporary.resolve(row.getCollection() + "_" + row.getSize() + ".log");
                tenFiles.add(new DataFile(pretendLog, Collections.singletonList(row), new ViewSettings()));
            }
            GraphPanel combinedLogs = new GraphPanel(Measurement.Metric.ADD_AVERAGE);
            combinedLogs.setFont(new Font("SansSerif", Font.PLAIN, 12));
            combinedLogs.setData(tenFiles);
            java.lang.reflect.Method seriesMethod = GraphPanel.class.getDeclaredMethod("createSeries");
            seriesMethod.setAccessible(true);
            java.util.Map<?, ?> grouped = (java.util.Map<?, ?>) seriesMethod.invoke(combinedLogs);
            check(grouped.size() == 2, "Десять логов объединяются в две линии коллекций");
            for (Object points : grouped.values()) {
                check(((List<?>) points).size() == 5, "По пять точек на каждом графике");
            }
            combinedLogs.setSize(590, 350);
            BufferedImage logImage = new BufferedImage(590, 350, BufferedImage.TYPE_INT_RGB);
            Graphics2D logGraphics = logImage.createGraphics();
            combinedLogs.paint(logGraphics);
            logGraphics.dispose();
            java.lang.reflect.Field hotspotsField = GraphPanel.class.getDeclaredField("hotspots");
            hotspotsField.setAccessible(true);
            List<?> markers = (List<?>) hotspotsField.get(combinedLogs);
            check(markers.size() == 10, "У всех десяти измерений есть маркеры для подсказок");
            Object firstMarker = markers.get(0);
            java.lang.reflect.Field markerX = firstMarker.getClass().getDeclaredField("x");
            java.lang.reflect.Field markerY = firstMarker.getClass().getDeclaredField("y");
            markerX.setAccessible(true);
            markerY.setAccessible(true);
            int hx = markerX.getInt(firstMarker);
            int hy = markerY.getInt(firstMarker);
            java.awt.event.MouseEvent hover = new java.awt.event.MouseEvent(combinedLogs,
                    java.awt.event.MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0,
                    hx, hy, 0, false);
            String tooltip = combinedLogs.getToolTipText(hover);
            check(tooltip != null && tooltip.contains("N = ") && tooltip.contains("нс"),
                    "Подсказка мыши показывает размер и время");

            DataFile second = io.read(log);
            Path combined = temporary.resolve("combined.lab5");
            io.saveBundle(combined, Arrays.asList(original, second), view);
            DataFile combinedRead = io.read(combined);
            check(combinedRead.getMeasurements().size() == 11, "Несколько источников сохранены вместе");
            check(!combinedRead.getMeasurements().get(0).getSourceTag().isEmpty(),
                    "В объединённом файле сохранены имена источников");
            Path bundledLogs = temporary.resolve("ten_logs.lab5");
            io.saveBundle(bundledLogs, tenFiles, view);
            GraphPanel reopenedLogs = new GraphPanel(Measurement.Metric.ADD_TOTAL);
            reopenedLogs.setData(Collections.singletonList(io.read(bundledLogs)));
            java.util.Map<?, ?> regrouped = (java.util.Map<?, ?>) seriesMethod.invoke(reopenedLogs);
            check(regrouped.size() == 2, "При повторной загрузке десять LOG остаются двумя линиями");

            Path semicolon = temporary.resolve("log;set.csv");
            Files.copy(csv, semicolon);
            Path escapedBundle = temporary.resolve("escaped.lab5");
            io.saveBundle(escapedBundle, Collections.singletonList(io.read(semicolon)), view);
            check(io.read(escapedBundle).getMeasurements().get(0).getSourceTag().equals("log;set.csv"),
                    "Имена источников с точкой с запятой сохраняются без порчи CSV");

            for (ViewSettings.Scale xs : ViewSettings.Scale.values()) {
                for (ViewSettings.Scale ys : ViewSettings.Scale.values()) {
                    ViewSettings settings = new ViewSettings();
                    settings.setXScale(xs);
                    settings.setYScale(ys);
                    settings.setZoom(2.5);
                    for (Measurement.Metric metric : Measurement.Metric.values()) {
                        GraphPanel panel = new GraphPanel(metric);
                        panel.setFont(new Font("SansSerif", Font.PLAIN, 12));
                        panel.setData(Arrays.asList(original, second));
                        panel.setSettings(settings);
                        panel.setSize(590, 350);
                        BufferedImage image = new BufferedImage(590, 350, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D graphics = image.createGraphics();
                        panel.paint(graphics);
                        graphics.dispose();
                        check(image.getRGB(220, 45) != 0, "Java2D рисует график " + metric + "/" + xs + "/" + ys);
                    }
                }
            }
            if (args.length > 0 && args[0].equals("--preview")) {
                createPreview(original, Paths.get("docs", "preview.png"));
                System.out.println("Создан docs/preview.png");
            }
            System.out.println("Все тесты пройдены. Проверок: " + passed);
        } finally {
            deleteDirectory(temporary);
        }
    }

    private static Measurement find(DataFile file, String collection, int n) {
        for (Measurement row : file.getMeasurements()) {
            if (row.getCollection().equals(collection) && row.getSize() == n) return row;
        }
        return null;
    }

    private interface TestCall { void execute() throws Exception; }
    private static void expectIOException(TestCall test, String message) throws Exception {
        try { test.execute(); }
        catch (IOException expected) { check(true, message); return; }
        throw new AssertionError("Не вызвано ожидаемое исключение: " + message);
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        passed++;
    }

    private static void createPreview(DataFile original, Path output) throws IOException {
        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.setBackground(java.awt.Color.WHITE);
        for (Measurement.Metric metric : Measurement.Metric.values()) {
            GraphPanel graph = new GraphPanel(metric);
            graph.setFont(new Font("SansSerif", Font.PLAIN, 12));
            graph.setData(Collections.singletonList(original));
            panel.add(graph);
        }
        panel.setSize(1250, 800);
        panel.doLayout();
        for (java.awt.Component item : panel.getComponents()) {
            item.setSize(item.getBounds().getSize());
        }
        BufferedImage image = new BufferedImage(1250, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        panel.paint(graphics);
        graphics.dispose();
        if (output.getParent() != null) Files.createDirectories(output.getParent());
        ImageIO.write(image, "png", output.toFile());
    }

    private static void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;
        List<Path> paths = new ArrayList<Path>();
        try (java.util.stream.Stream<Path> stream = Files.walk(path)) {
            stream.forEach(paths::add);
        }
        paths.sort(Collections.reverseOrder());
        for (Path item : paths) Files.deleteIfExists(item);
    }
}
