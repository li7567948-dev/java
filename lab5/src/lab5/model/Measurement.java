package lab5.model;

/** Одна строка сводных результатов эксперимента ЛР3. Все времена — наносекунды. */
public final class Measurement {
    private final String sourceTag;
    private final String collection;
    private final int size;
    private final double addTotalNs;
    private final double addAverageNs;
    private final Double addMedianNs;
    private final double changeTotalNs;
    private final double changeAverageNs;
    private final Double changeMedianNs;

    public Measurement(String sourceTag, String collection, int size,
            double addTotalNs, double addAverageNs,
            double changeTotalNs, double changeAverageNs) {
        this(sourceTag, collection, size, addTotalNs, addAverageNs, null,
                changeTotalNs, changeAverageNs, null);
    }

    public Measurement(String sourceTag, String collection, int size,
            double addTotalNs, double addAverageNs, Double addMedianNs,
            double changeTotalNs, double changeAverageNs, Double changeMedianNs) {
        if (collection == null || collection.trim().isEmpty()) {
            throw new IllegalArgumentException("Не указан тип коллекции");
        }
        if (size <= 0 || !valid(addTotalNs) || !valid(addAverageNs)
                || !valid(changeTotalNs) || !valid(changeAverageNs)
                || (addMedianNs != null && !valid(addMedianNs))
                || (changeMedianNs != null && !valid(changeMedianNs))) {
            throw new IllegalArgumentException("Недопустимое число элементов или время операции");
        }
        this.sourceTag = sourceTag == null ? "" : sourceTag.trim();
        this.collection = collection.trim();
        this.size = size;
        this.addTotalNs = addTotalNs;
        this.addAverageNs = addAverageNs;
        this.addMedianNs = addMedianNs;
        this.changeTotalNs = changeTotalNs;
        this.changeAverageNs = changeAverageNs;
        this.changeMedianNs = changeMedianNs;
    }

    private static boolean valid(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0;
    }

    public String getSourceTag() { return sourceTag; }
    public String getCollection() { return collection; }
    public int getSize() { return size; }
    public double getAddTotalNs() { return addTotalNs; }
    public double getAddAverageNs() { return addAverageNs; }
    public Double getAddMedianNs() { return addMedianNs; }
    public double getChangeTotalNs() { return changeTotalNs; }
    public double getChangeAverageNs() { return changeAverageNs; }
    public Double getChangeMedianNs() { return changeMedianNs; }

    public enum Metric {
        ADD_AVERAGE("Среднее время добавления", "нс / операция"),
        ADD_TOTAL("Общее время добавления", "нс"),
        CHANGE_AVERAGE("Среднее время изменения", "нс / операция"),
        CHANGE_TOTAL("Общее время изменения", "нс");

        private final String title;
        private final String unit;
        Metric(String title, String unit) { this.title = title; this.unit = unit; }
        public String getTitle() { return title; }
        public String getUnit() { return unit; }
    }

    public double getMetric(Metric metric) {
        switch (metric) {
            case ADD_AVERAGE: return addAverageNs;
            case ADD_TOTAL: return addTotalNs;
            case CHANGE_AVERAGE: return changeAverageNs;
            case CHANGE_TOTAL: return changeTotalNs;
            default: throw new IllegalArgumentException("Неизвестная характеристика");
        }
    }
}
