package model;


/** Результат проверки с независимой копией исходных данных. */
public final class CheckResult {
    private final Vehicle vehicle;
    private final boolean allowed;
    private final String reason;
    private final int checkTime;

    public CheckResult(Vehicle vehicle) {
        this.vehicle = VehicleFactory.copyOf(vehicle);
        allowed = this.vehicle.pass();
        reason = this.vehicle.getReason();
        checkTime = this.vehicle.getCheckTime();
    }

    public Vehicle getVehicle() {
        return VehicleFactory.copyOf(vehicle);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getReason() {
        return reason;
    }

    public int getCheckTime() {
        return checkTime;
    }
}
