package model;

public final class PassengerCar extends Vehicle {
    public PassengerCar(String registration, Driver driver, boolean extraInspection) {
        super(registration, driver, extraInspection);
    }
    @Override public VehicleType getType() { return VehicleType.PASSENGER; }
}
