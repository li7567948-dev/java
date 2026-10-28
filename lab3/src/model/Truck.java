package model;

public final class Truck extends Vehicle {
    public Truck(String registration, Driver driver, boolean extraInspection) {
        super(registration, driver, extraInspection);
    }
    @Override public VehicleType getType() { return VehicleType.TRUCK; }
}
