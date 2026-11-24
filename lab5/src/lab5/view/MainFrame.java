package lab5.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import lab5.io.ErrorHandler;
import lab5.io.MeasurementFileService;
import lab5.model.DataFile;
import lab5.model.Measurement;
import lab5.model.ViewSettings;

/** Главное окно лабораторной работы №5; только GUI и обработка событий. */
@SuppressWarnings("serial") // Окно Swing предназначено для отображения, не для сериализации.
public final class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String SEPARATE = "Раздельно (по файлам)";
    private static final String TOGETHER = "Совместно (все файлы)";

    private final MeasurementFileService fileService = new MeasurementFileService();
    private final ErrorHandler errors = new ErrorHandler(Paths.get("lab5_errors.log"));
    private final List<DataFile> loadedFiles = new ArrayList<DataFile>();
    private final List<GraphPanel> charts = new ArrayList<GraphPanel>();
    private ViewSettings currentSettings = new ViewSettings();
    private boolean updatingControls;

    private final JComboBox<String> viewMode = new JComboBox<String>(new String[] {SEPARATE, TOGETHER});
    private final JComboBox<DataFile> fileSelector = new JComboBox<DataFile>();
    private final JComboBox<ViewSettings.Scale> xScale = new JComboBox<ViewSettings.Scale>(ViewSettings.Scale.values());
    private final JComboBox<ViewSettings.Scale> yScale = new JComboBox<ViewSettings.Scale>(ViewSettings.Scale.values());
    private final JCheckBox legend = new JCheckBox("Легенда", true);
    private final JSlider zoomSlider = new JSlider(50, 800, 100);
    private final JLabel zoomLabel = new JLabel("100%");
    private final JLabel status = new JLabel("Готово");
    private final JButton openButton = new JButton("Открыть файлы...");
    private final JButton saveSettingsButton = new JButton("Сохранить настройки");
    private final JButton saveBundleButton = new JButton("Сохранить данные + настройки...");
    private final JButton removeButton = new JButton("Убрать файл");

    public MainFrame(List<Path> initialFiles) {
        super("Лабораторная работа №5 — Графики Java2D по данным ЛР3");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(920, 640));
        setSize(1370, 880);
        setLocationRelativeTo(null);
        createInterface();
        connectEvents();
        loadStartupFiles(initialFiles);
    }

    private void createInterface() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(11, 12, 9, 12));
        content.setBackground(new Color(244, 247, 250));
        setContentPane(content);

        JPanel tools = new JPanel();
        tools.setOpaque(false);
        tools.setLayout(new BoxLayout(tools, BoxLayout.Y_AXIS));
        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 4));
        fileRow.setOpaque(false);
        fileRow.add(openButton);
        fileRow.add(saveBundleButton);
        fileRow.add(saveSettingsButton);
        fileRow.add(removeButton);
        fileRow.add(new JLabel("Показ:"));
        fileRow.add(viewMode);
        fileSelector.setPreferredSize(new Dimension(185, 27));
        fileRow.add(fileSelector);
        tools.add(fileRow);

        JPanel displayRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        displayRow.setOpaque(false);
        displayRow.add(new JLabel("Масштаб:"));
        JButton reduce = new JButton("−");
        JButton increase = new JButton("+");
        JButton reset = new JButton("100% / Сброс сдвига");
        reduce.addActionListener(event -> zoomBy(0.8));
        increase.addActionListener(event -> zoomBy(1.25));
        reset.addActionListener(event -> {
            zoomSlider.setValue(100);
            for (GraphPanel chart : charts) chart.resetPan();
        });
        zoomSlider.setPreferredSize(new Dimension(152, 27));
        displayRow.add(reduce);
        displayRow.add(zoomSlider);
        displayRow.add(increase);
        displayRow.add(zoomLabel);
        displayRow.add(reset);
        displayRow.add(legend);
        displayRow.add(new JLabel("Ось X:"));
        displayRow.add(xScale);
        displayRow.add(new JLabel("Ось Y:"));
        displayRow.add(yScale);
        tools.add(displayRow);
        content.add(tools, BorderLayout.NORTH);

        JPanel graphGrid = new JPanel(new GridLayout(2, 2, 10, 10));
        graphGrid.setOpaque(false);
        for (Measurement.Metric metric : Measurement.Metric.values()) {
            GraphPanel chart = new GraphPanel(metric);
            chart.setZoomChangeListener(value -> zoomSlider.setValue((int) Math.round(100 * value)));
            charts.add(chart);
            graphGrid.add(chart);
        }
        graphGrid.setPreferredSize(new Dimension(1140, 710));
        JScrollPane scrollPane = new JScrollPane(graphGrid);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        content.add(scrollPane, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        status.setFont(status.getFont().deriveFont(Font.PLAIN, 12f));
        footer.add(status, BorderLayout.WEST);
        JLabel tip = new JLabel("Мышь: точка — значение; колёсико — масштаб; перетаскивание — сдвиг");
        tip.setFont(tip.getFont().deriveFont(11f));
        footer.add(tip, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);

        JMenuBar menu = new JMenuBar();
        JMenu file = new JMenu("Файл");
        JMenuItem open = new JMenuItem("Загрузить данные ЛР3...");
        open.addActionListener(event -> chooseFiles());
        JMenuItem save = new JMenuItem("Сохранить данные и настройки (.lab5)...");
        save.addActionListener(event -> saveDataBundle());
        JMenuItem remember = new JMenuItem("Сохранить настройки текущих файлов");
        remember.addActionListener(event -> saveViewSettings());
        JMenuItem close = new JMenuItem("Выход");
        close.addActionListener(event -> dispose());
        file.add(open); file.add(save); file.add(remember); file.addSeparator(); file.add(close);
        JMenu help = new JMenu("Справка");
        JMenuItem about = new JMenuItem("Как пользоваться");
        about.addActionListener(this::showHelp);
        help.add(about);
        menu.add(file); menu.add(help);
        setJMenuBar(menu);
        applyControls(currentSettings);
    }

    private void connectEvents() {
        openButton.addActionListener(event -> chooseFiles());
        saveSettingsButton.addActionListener(event -> saveViewSettings());
        saveBundleButton.addActionListener(event -> saveDataBundle());
        removeButton.addActionListener(event -> {
            DataFile selected = (DataFile) fileSelector.getSelectedItem();
            if (selected != null) {
                loadedFiles.remove(selected);
                fileSelector.removeItem(selected);
                selectSettingsForCurrentFile();
                redraw();
            }
        });
        fileSelector.addActionListener(event -> {
            if (!updatingControls) {
                selectSettingsForCurrentFile();
                redraw();
            }
        });
        viewMode.addActionListener(event -> {
            fileSelector.setEnabled(isSeparate());
            redraw();
        });
        zoomSlider.addChangeListener(event -> {
            if (!updatingControls) {
                currentSettings.setZoom(zoomSlider.getValue() / 100.0);
                zoomLabel.setText(zoomSlider.getValue() + "%");
                updateChartsSettings();
            }
        });
        legend.addActionListener(event -> {
            if (!updatingControls) {
                currentSettings.setLegendVisible(legend.isSelected());
                updateChartsSettings();
            }
        });
        xScale.addActionListener(event -> {
            if (!updatingControls && xScale.getSelectedItem() != null) {
                currentSettings.setXScale((ViewSettings.Scale) xScale.getSelectedItem());
                updateChartsSettings();
            }
        });
        yScale.addActionListener(event -> {
            if (!updatingControls && yScale.getSelectedItem() != null) {
                currentSettings.setYScale((ViewSettings.Scale) yScale.getSelectedItem());
                updateChartsSettings();
            }
        });
    }

    private void loadStartupFiles(List<Path> initialFiles) {
        List<Path> paths = new ArrayList<Path>(initialFiles);
        if (paths.isEmpty()) {
            Path preferred = Paths.get("data", "lab3_summary.csv");
            if (!Files.isRegularFile(preferred)) preferred = Paths.get("lab5", "data", "lab3_summary.csv");
            if (Files.isRegularFile(preferred)) paths.add(preferred);
        }
        for (Path path : paths) {
            try { addLoadedFile(fileService.read(path)); }
            catch (Exception exception) { showError(exception); }
        }
        if (!loadedFiles.isEmpty()) selectSettingsForCurrentFile();
        redraw();
    }

    private void chooseFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Выберите CSV, LOG ЛР3 либо набор LAB5");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Данные ЛР3/ЛР5 (*.csv, *.log, *.lab5)",
                "csv", "log", "lab5"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File[] selected = chooser.getSelectedFiles();
        if (selected.length == 0 && chooser.getSelectedFile() != null) {
            selected = new File[] {chooser.getSelectedFile()};
        }
        final File[] files = selected;
        openButton.setEnabled(false);
        status.setText("Чтение файлов...");
        new SwingWorker<List<DataFile>, Void>() {
            private final List<Exception> failures = new ArrayList<Exception>();
            @Override protected List<DataFile> doInBackground() {
                List<DataFile> accepted = new ArrayList<DataFile>();
                for (File input : files) {
                    try { accepted.add(fileService.read(input.toPath())); }
                    catch (Exception exception) { failures.add(exception); }
                }
                return accepted;
            }
            @Override protected void done() {
                openButton.setEnabled(true);
                try {
                    for (DataFile file : get()) addLoadedFile(file);
                    if (!loadedFiles.isEmpty()) selectSettingsForCurrentFile();
                    redraw();
                    for (Exception failure : failures) showError(failure);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError(exception);
                } catch (ExecutionException exception) {
                    showError(new IOException("Ошибка загрузки", exception.getCause()));
                }
            }
        }.execute();
    }

    private void addLoadedFile(DataFile data) {
        for (DataFile old : new ArrayList<DataFile>(loadedFiles)) {
            if (old.getPath().equals(data.getPath())) {
                loadedFiles.remove(old);
                fileSelector.removeItem(old);
            }
        }
        loadedFiles.add(data);
        updatingControls = true;
        try {
            fileSelector.addItem(data);
            fileSelector.setSelectedItem(data);
        } finally { updatingControls = false; }
    }

    private void selectSettingsForCurrentFile() {
        DataFile selected = (DataFile) fileSelector.getSelectedItem();
        if (selected != null) applyControls(selected.getSettings());
    }

    private void applyControls(ViewSettings saved) {
        currentSettings = new ViewSettings(saved);
        updatingControls = true;
        try {
            zoomSlider.setValue((int) Math.round(saved.getZoom() * 100));
            zoomLabel.setText(zoomSlider.getValue() + "%");
            legend.setSelected(saved.isLegendVisible());
            xScale.setSelectedItem(saved.getXScale());
            yScale.setSelectedItem(saved.getYScale());
        } finally { updatingControls = false; }
        updateChartsSettings();
    }

    private void updateChartsSettings() {
        for (GraphPanel chart : charts) chart.setSettings(currentSettings);
    }

    private boolean isSeparate() { return viewMode.getSelectedIndex() == 0; }

    private void redraw() {
        List<DataFile> visible;
        if (isSeparate()) {
            DataFile selected = (DataFile) fileSelector.getSelectedItem();
            visible = selected == null ? Collections.<DataFile>emptyList()
                    : Collections.singletonList(selected);
        } else visible = new ArrayList<DataFile>(loadedFiles);
        for (GraphPanel chart : charts) {
            chart.setData(visible);
            chart.setSettings(currentSettings);
        }
        fileSelector.setEnabled(isSeparate() && !loadedFiles.isEmpty());
        removeButton.setEnabled(!loadedFiles.isEmpty());
        saveSettingsButton.setEnabled(!loadedFiles.isEmpty());
        saveBundleButton.setEnabled(!loadedFiles.isEmpty());
        int rows = 0;
        for (DataFile file : visible) rows += file.getMeasurements().size();
        status.setText("Загружено файлов: " + loadedFiles.size() + " | Показано точек: "
                + rows + " на каждом графике | Ошибок: " + errors.getErrorCount());
    }

    private void zoomBy(double factor) {
        int value = (int) Math.round(zoomSlider.getValue() * factor);
        zoomSlider.setValue(Math.max(50, Math.min(800, value)));
    }

    private void saveViewSettings() {
        if (loadedFiles.isEmpty()) return;
        int count = 0;
        for (DataFile file : new ArrayList<DataFile>(loadedFiles)) {
            try {
                fileService.saveSettings(file.getPath(), currentSettings);
                // Используем те же проверенные измерения, без повторного чтения 100 000 строк лога.
                DataFile refreshed = new DataFile(file.getPath(), file.getMeasurements(), currentSettings);
                int index = loadedFiles.indexOf(file);
                loadedFiles.set(index, refreshed);
                updatingControls = true;
                try {
                    int selected = fileSelector.getSelectedIndex();
                    fileSelector.removeItemAt(index);
                    fileSelector.insertItemAt(refreshed, index);
                    fileSelector.setSelectedIndex(selected);
                } finally { updatingControls = false; }
                count++;
            } catch (Exception exception) { showError(exception); }
        }
        redraw();
        if (count > 0) {
            JOptionPane.showMessageDialog(this,
                    "Настройки сохранены для " + count + " файл(ов).\n"
                    + "Для CSV/LOG рядом создан .view.properties; для .lab5 настройки находятся внутри файла.",
                    "Сохранение", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void saveDataBundle() {
        if (loadedFiles.isEmpty()) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Сохранить измерения и настройки в один файл");
        chooser.setSelectedFile(new File("lab5_result.lab5"));
        chooser.setFileFilter(new FileNameExtensionFilter("Набор данных лабораторной №5 (*.lab5)", "lab5"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path target = chooser.getSelectedFile().toPath();
        if (!target.toString().toLowerCase(Locale.ROOT).endsWith(".lab5")) {
            target = Paths.get(target.toString() + ".lab5");
        }
        if (Files.exists(target)) {
            int choice = JOptionPane.showConfirmDialog(this, "Перезаписать файл "
                    + target.getFileName() + "?", "Подтверждение", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) return;
        }
        try {
            List<DataFile> files = new ArrayList<DataFile>(loadedFiles);
            fileService.saveBundle(target, files, currentSettings);
            JOptionPane.showMessageDialog(this,
                    "Измерения и настройки сохранены в одном файле:\n" + target.toAbsolutePath(),
                    "Сохранено", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception exception) { showError(exception); }
    }

    private void showHelp(ActionEvent event) {
        JOptionPane.showMessageDialog(this,
                "1. Откройте results/summary.csv из ЛР3 или выделите все 10 LOG.\n"
                + "2. Выберите отдельный файл либо совместный просмотр.\n"
                + "3. Измените масштаб, легенду и типы осей X/Y.\n"
                + "4. Наведите мышь на точку — появится её локальное значение.\n"
                + "5. Колёсико меняет масштаб; левая кнопка и перемещение сдвигают график.\n"
                + "6. Сохраните данные и настройки в один файл .lab5 и загрузите его позже.\n"
                + "\nВ ЛР3 использованы ArrayList и HashMap, операции add/put и set/replace.\n"
                + "Значения измеряются в наносекундах (нс).",
                "Инструкция — ЛР №5", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(Exception exception) {
        String message = errors.register(exception);
        status.setText(message);
        JOptionPane.showMessageDialog(this, message, "Ошибка чтения/сохранения",
                JOptionPane.ERROR_MESSAGE);
    }
}
