package model;

public final class Bus extends Vehicle {
    public Bus(String registration, Driver driver, boolean extraInspection) {
        super(registration, driver, extraInspection);
    }
    @Override public VehicleType getType() { return VehicleType.BUS; }
}
