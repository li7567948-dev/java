package lab5.model;

/** Настройки, которые сохраняются вместе с экспериментальными данными. */
public final class ViewSettings {
    public enum Scale { LINEAR, LOGARITHMIC }

    private double zoom = 1.0;
    private boolean legendVisible = true;
    private Scale xScale = Scale.LOGARITHMIC;
    private Scale yScale = Scale.LINEAR;

    public ViewSettings() { }

    public ViewSettings(ViewSettings other) {
        zoom = other.zoom;
        legendVisible = other.legendVisible;
        xScale = other.xScale;
        yScale = other.yScale;
    }

    public double getZoom() { return zoom; }
    public void setZoom(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.5 || value > 8.0) {
            throw new IllegalArgumentException("Масштаб должен быть от 50% до 800%");
        }
        zoom = value;
    }
    public boolean isLegendVisible() { return legendVisible; }
    public void setLegendVisible(boolean value) { legendVisible = value; }
    public Scale getXScale() { return xScale; }
    public void setXScale(Scale value) {
        if (value == null) throw new IllegalArgumentException("Неизвестный тип оси X");
        xScale = value;
    }
    public Scale getYScale() { return yScale; }
    public void setYScale(Scale value) {
        if (value == null) throw new IllegalArgumentException("Неизвестный тип оси Y");
        yScale = value;
    }
}
