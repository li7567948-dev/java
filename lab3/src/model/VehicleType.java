package model;

/** Types of transport serviced at the checkpoint. */
public enum VehicleType {
    PASSENGER("Легковой автомобиль", 3),
    BUS("Автобус", 6),
    TRUCK("Грузовик", 8);

    private final String label;
    private final int baseMinutes;

    VehicleType(String label, int baseMinutes) {
        this.label = label;
        this.baseMinutes = baseMinutes;
    }

    public String getLabel() { return label; }
    public int getBaseMinutes() { return baseMinutes; }
}
