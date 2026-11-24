package lab5.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.ToolTipManager;

import lab5.model.DataFile;
import lab5.model.Measurement;
import lab5.model.ViewSettings;

/** Одна интерактивная диаграмма: Java2D, масштабирование, оси, легенда и подсказки. */
@SuppressWarnings("serial") // Компонент графика не записывается через ObjectOutputStream.
public final class GraphPanel extends JComponent {
    private static final long serialVersionUID = 1L;
    private static final Color INK = new Color(36, 52, 70);
    private static final Color GRID = new Color(228, 235, 241);
    private static final Color[] COLORS = {
        new Color(31, 102, 181), new Color(221, 111, 33),
        new Color(20, 143, 120), new Color(143, 76, 163),
        new Color(189, 67, 79), new Color(121, 101, 41)
    };
    private static final int[] EXPERIMENT_SIZES = {10, 100, 1000, 10000, 100000};

    private final Measurement.Metric metric;
    private List<DataFile> files = Collections.emptyList();
    private ViewSettings settings = new ViewSettings();
    private final List<Hotspot> hotspots = new ArrayList<Hotspot>();
    private double offsetX;
    private double offsetY;
    private int dragStartX;
    private int dragStartY;
    private int hoverX = -1;
    private int hoverY = -1;
    private boolean dragging;

    public GraphPanel(Measurement.Metric metric) {
        if (metric == null) throw new IllegalArgumentException("Не указан тип графика");
        this.metric = metric;
        setPreferredSize(new Dimension(550, 350));
        setOpaque(true);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(new Color(210, 221, 230)));
        setToolTipText(" ");
        ToolTipManager.sharedInstance().registerComponent(this);
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON1) {
                    dragging = true;
                    dragStartX = event.getX();
                    dragStartY = event.getY();
                }
            }
            @Override public void mouseReleased(MouseEvent event) { dragging = false; }
            @Override public void mouseExited(MouseEvent event) {
                hoverX = -1;
                hoverY = -1;
                repaint();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent event) {
                hoverX = event.getX(); hoverY = event.getY(); repaint();
            }
            @Override public void mouseDragged(MouseEvent event) {
                if (dragging) {
                    offsetX += event.getX() - dragStartX;
                    offsetY += event.getY() - dragStartY;
                    dragStartX = event.getX(); dragStartY = event.getY();
                    repaint();
                }
            }
        });
        addMouseWheelListener(event -> {
            double target = settings.getZoom() * (event.getWheelRotation() < 0 ? 1.25 : 0.8);
            target = Math.min(8.0, Math.max(0.5, target));
            if (Math.abs(target - settings.getZoom()) > 0.01) {
                ViewSettings updated = new ViewSettings(settings);
                updated.setZoom(target);
                if (onZoomChange != null) onZoomChange.zoomChanged(target);
                else setSettings(updated);
            }
        });
    }

    public interface ZoomChangeListener { void zoomChanged(double zoom); }
    private ZoomChangeListener onZoomChange;
    public void setZoomChangeListener(ZoomChangeListener listener) { onZoomChange = listener; }

    public void setData(List<DataFile> data) {
        this.files = new ArrayList<DataFile>(data);
        offsetX = 0;
        offsetY = 0;
        repaint();
    }

    public void setSettings(ViewSettings current) {
        settings = new ViewSettings(current);
        repaint();
    }

    public void resetPan() { offsetX = 0; offsetY = 0; repaint(); }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            paintChart(g2);
        } finally {
            g2.dispose();
        }
    }

    private void paintChart(Graphics2D g2) {
        hotspots.clear();
        int width = getWidth();
        int height = getHeight();
        // JComponent без UI delegate обязан самостоятельно залить фон.
        g2.setColor(getBackground());
        g2.fillRect(0, 0, width, height);
        if (width < 120 || height < 140) return;
        g2.setColor(INK);
        g2.setFont(getFont().deriveFont(Font.BOLD, 15f));
        g2.drawString(metric.getTitle(), 18, 27);
        g2.setFont(getFont().deriveFont(Font.PLAIN, 11f));
        g2.setColor(new Color(105, 122, 140));
        g2.drawString("(ЛР3: set / replace, не remove)", 18, 43);

        LinkedHashMap<String, List<Measurement>> series = createSeries();
        if (series.isEmpty()) {
            g2.setColor(INK);
            g2.setFont(getFont().deriveFont(13f));
            g2.drawString("Нет данных. Откройте CSV или LOG ЛР3.", 34, height / 2);
            return;
        }

        int left = 76;
        int legendBottom = settings.isLegendVisible()
                ? paintLegend(g2, series, left, 58, width - 24) : 43;
        int top = Math.max(60, legendBottom + 20);
        int right = width - 25;
        int bottom = height - 62;
        if (right - left < 80 || bottom - top < 80) return;

        double maxValue = 0;
        double minPositive = Double.POSITIVE_INFINITY;
        for (List<Measurement> points : series.values()) {
            for (Measurement row : points) {
                double value = row.getMetric(metric);
                maxValue = Math.max(maxValue, value);
                if (value > 0) minPositive = Math.min(minPositive, value);
            }
        }
        if (!(maxValue > 0)) maxValue = 1;
        if (!Double.isFinite(minPositive)) minPositive = 0.1;
        double yMin = settings.getYScale() == ViewSettings.Scale.LINEAR
                ? 0.0 : Math.log10(minPositive) - 0.35;
        double yMax = settings.getYScale() == ViewSettings.Scale.LINEAR
                ? maxValue * 1.12 : Math.log10(maxValue) + 0.35;
        if (yMax <= yMin) yMax = yMin + 1;
        double xMin = settings.getXScale() == ViewSettings.Scale.LINEAR ? 0 : Math.log10(10);
        double xMax = settings.getXScale() == ViewSettings.Scale.LINEAR ? 110000 : Math.log10(100000) + 0.12;

        g2.setColor(new Color(250, 252, 254));
        g2.fillRect(left, top, right - left, bottom - top);
        paintGrid(g2, left, top, right, bottom, xMin, xMax, yMin, yMax);

        Graphics2D plot = (Graphics2D) g2.create();
        try {
            plot.setClip(left, top, right - left + 1, bottom - top + 1);
            int seriesNumber = 0;
            for (Map.Entry<String, List<Measurement>> group : series.entrySet()) {
                Color color = getSeriesColor(group.getKey(), seriesNumber);
                plot.setColor(color);
                Stroke stroke = new BasicStroke(2.2f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND, 10, seriesNumber < 2 ? null : new float[] {7, 4}, 0);
                plot.setStroke(stroke);
                List<Measurement> points = group.getValue();
                double px = 0, py = 0;
                boolean previousValid = false;
                for (Measurement item : points) {
                    double y = item.getMetric(metric);
                    if (settings.getYScale() == ViewSettings.Scale.LOGARITHMIC && y <= 0) {
                        previousValid = false;
                        continue;
                    }
                    double sx = drawX(item.getSize(), xMin, xMax, left, right);
                    double sy = drawY(y, yMin, yMax, top, bottom);
                    if (previousValid) plot.draw(new Line2D.Double(px, py, sx, sy));
                    px = sx; py = sy; previousValid = true;
                    plot.fill(new Ellipse2D.Double(sx - 4.5, sy - 4.5, 9, 9));
                    plot.setColor(Color.WHITE);
                    plot.fill(new Ellipse2D.Double(sx - 1.6, sy - 1.6, 3.2, 3.2));
                    plot.setColor(color);
                    hotspots.add(new Hotspot((int) Math.round(sx), (int) Math.round(sy),
                            group.getKey(), item));
                }
                seriesNumber++;
            }
        } finally { plot.dispose(); }
        g2.setColor(INK);
        g2.setFont(getFont().deriveFont(11f));
        String xText = "Количество элементов N";
        g2.drawString(xText, (left + right - g2.getFontMetrics().stringWidth(xText)) / 2, height - 20);
        g2.drawString(metric.getUnit(), 12, top - 10);

        Hotspot active = closest(hoverX, hoverY);
        if (active != null) {
            g2.setColor(new Color(37, 49, 63, 90));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawOval(active.x - 8, active.y - 8, 16, 16);
        }
    }

    private void paintGrid(Graphics2D g2, int left, int top, int right, int bottom,
            double xMin, double xMax, double yMin, double yMax) {
        g2.setFont(getFont().deriveFont(10.5f));
        FontMetrics metrics = g2.getFontMetrics();
        int lastLabelRight = Integer.MIN_VALUE;
        if (settings.getXScale() == ViewSettings.Scale.LINEAR) {
            for (int index = 0; index <= 4; index++) {
                int n = index * 25000;
                int x = (int) Math.round(drawX(n, xMin, xMax, left, right));
                if (x >= left && x <= right) {
                    verticalGrid(g2, x, top, bottom);
                    String label = Integer.toString(n);
                    int start = x - metrics.stringWidth(label) / 2;
                    if (start > lastLabelRight + 3) {
                        g2.setColor(INK); g2.drawString(label, start, bottom + 17);
                        lastLabelRight = start + metrics.stringWidth(label);
                    }
                }
            }
        } else {
            for (int n : EXPERIMENT_SIZES) {
                int x = (int) Math.round(drawX(n, xMin, xMax, left, right));
                if (x >= left && x <= right) {
                    verticalGrid(g2, x, top, bottom);
                    String label = Integer.toString(n);
                    int start = x - metrics.stringWidth(label) / 2;
                    if (start > lastLabelRight + 5) {
                        g2.setColor(INK); g2.drawString(label, start, bottom + 17);
                        lastLabelRight = start + metrics.stringWidth(label);
                    }
                }
            }
        }
        if (settings.getYScale() == ViewSettings.Scale.LINEAR) {
            for (int tick = 0; tick <= 4; tick++) {
                double value = (yMax - yMin) * tick / 4.0;
                int y = (int) Math.round(drawY(value, yMin, yMax, top, bottom));
                if (y >= top && y <= bottom) {
                    horizontalGrid(g2, left, right, y);
                    String label = compact(value);
                    g2.setColor(INK); g2.drawString(label, left - 8 - metrics.stringWidth(label), y + 4);
                }
            }
        } else {
            int from = Math.max(-12, (int) Math.floor(yMin));
            int to = Math.min(18, (int) Math.ceil(yMax));
            for (int power = from; power <= to; power++) {
                double value = Math.pow(10, power);
                int y = (int) Math.round(drawY(value, yMin, yMax, top, bottom));
                if (y >= top && y <= bottom) {
                    horizontalGrid(g2, left, right, y);
                    String label = "10^" + power;
                    g2.setColor(INK); g2.drawString(label, left - 8 - metrics.stringWidth(label), y + 4);
                }
            }
        }
        g2.setColor(new Color(160, 174, 188));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(left, top, right - left, bottom - top);
    }

    private static void verticalGrid(Graphics2D g2, int x, int top, int bottom) {
        g2.setColor(GRID); g2.setStroke(new BasicStroke(1)); g2.drawLine(x, top, x, bottom);
    }
    private static void horizontalGrid(Graphics2D g2, int left, int right, int y) {
        g2.setColor(GRID); g2.setStroke(new BasicStroke(1)); g2.drawLine(left, y, right, y);
    }

    private double drawX(double value, double low, double high, int left, int right) {
        double transformed = settings.getXScale() == ViewSettings.Scale.LOGARITHMIC
                ? Math.log10(Math.max(value, 1e-12)) : value;
        return (left + right) / 2.0 + (right - left) *
                ((transformed - low) / (high - low) - 0.5) * settings.getZoom() + offsetX;
    }
    private double drawY(double value, double low, double high, int top, int bottom) {
        double transformed = settings.getYScale() == ViewSettings.Scale.LOGARITHMIC
                ? Math.log10(Math.max(value, 1e-12)) : value;
        return (top + bottom) / 2.0 - (bottom - top) *
                ((transformed - low) / (high - low) - 0.5) * settings.getZoom() + offsetY;
    }

    private LinkedHashMap<String, List<Measurement>> createSeries() {
        LinkedHashMap<String, List<Measurement>> result = new LinkedHashMap<String, List<Measurement>>();
        for (DataFile file : files) {
            for (Measurement measurement : file.getMeasurements()) {
                // Десять отдельных LOG-файлов ЛР3 образуют ДВЕ линии по коллекциям,
                // а не десять несвязанных одиночных точек.
                boolean rawLog = file.getName().toLowerCase(Locale.ROOT).endsWith(".log")
                        && measurement.getSourceTag().isEmpty();
                boolean bundledLog = measurement.getSourceTag().toLowerCase(Locale.ROOT).contains(".log");
                String origin;
                if (rawLog) {
                    String folder = file.getPath().getParent() == null ? "LOG"
                            : file.getPath().getParent().getFileName().toString();
                    origin = "LOG: " + folder;
                } else if (bundledLog) {
                    origin = "LOG: " + file.getName();
                } else {
                    origin = measurement.getSourceTag().isEmpty()
                            ? file.getName() : measurement.getSourceTag();
                }
                String label = measurement.getCollection() + " · " + origin;
                List<Measurement> items = result.get(label);
                if (items == null) {
                    items = new ArrayList<Measurement>();
                    result.put(label, items);
                }
                items.add(measurement);
            }
        }
        for (List<Measurement> points : result.values()) {
            Collections.sort(points, Comparator.comparingInt(Measurement::getSize));
        }
        return result;
    }

    private static Color getSeriesColor(String name, int number) {
        if (name.startsWith("ArrayList")) return number < 2 ? COLORS[0] : COLORS[number % COLORS.length];
        if (name.startsWith("HashMap")) return number < 2 ? COLORS[1] : COLORS[number % COLORS.length];
        return COLORS[number % COLORS.length];
    }

    /** Выводит ВСЕ обозначения, перенося их на следующие строки. */
    private int paintLegend(Graphics2D g2, LinkedHashMap<String, List<Measurement>> series,
            int left, int startY, int right) {
        g2.setFont(getFont().deriveFont(10.5f));
        FontMetrics metrics = g2.getFontMetrics();
        int index = 0;
        int x = left;
        int y = startY;
        for (String label : series.keySet()) {
            String shortLabel = label.length() > 35 ? label.substring(0, 32) + "..." : label;
            int length = metrics.stringWidth(shortLabel) + 28;
            if (x + length > right && x != left) {
                x = left;
                y += 15;
            }
            g2.setColor(getSeriesColor(label, index));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawLine(x, y - 4, x + 15, y - 4);
            g2.fillOval(x + 5, y - 7, 6, 6);
            g2.setColor(INK);
            g2.drawString(shortLabel, x + 22, y);
            x += length + 10;
            index++;
        }
        return y;
    }

    private Hotspot closest(int x, int y) {
        Hotspot result = null;
        double best = 13.0 * 13.0;
        for (Hotspot item : hotspots) {
            double distance = Math.pow(item.x - x, 2) + Math.pow(item.y - y, 2);
            if (distance < best) { best = distance; result = item; }
        }
        return result;
    }

    @Override public String getToolTipText(MouseEvent event) {
        Hotspot hovered = closest(event.getX(), event.getY());
        if (hovered == null) return null;
        String series = escapeHtml(hovered.series);
        Measurement measurement = hovered.measurement;
        return "<html><b>" + series + "</b>"
                + "<br>N = " + measurement.getSize()
                + "<br>" + escapeHtml(metric.getTitle()) + " = "
                + formatValue(measurement.getMetric(metric)) + " "
                + escapeHtml(metric.getUnit()) + "</html>";
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
    private static String formatValue(double value) { return String.format(Locale.US, "%.2f", value); }
    private static String compact(double value) {
        if (value >= 1000000) return String.format(Locale.US, "%.1e", value);
        if (value >= 1000) return String.format(Locale.US, "%.0f", value);
        if (value < 1) return String.format(Locale.US, "%.2f", value);
        return String.format(Locale.US, "%.1f", value);
    }

    private static final class Hotspot {
        private final int x, y;
        private final String series;
        private final Measurement measurement;
        private Hotspot(int x, int y, String series, Measurement measurement) {
            this.x = x; this.y = y; this.series = series; this.measurement = measurement;
        }
    }
}
