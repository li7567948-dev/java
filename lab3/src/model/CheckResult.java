package model;

import java.util.Objects;

/** Immutable outcome of checking one vehicle. */
public final class CheckResult {
    private final Vehicle vehicle;
    private final boolean allowed;
    private final String reason;
    private final int checkTime;

    public CheckResult(Vehicle vehicle) {
        this.vehicle = Objects.requireNonNull(vehicle, "Машина отсутствует");
        this.allowed = vehicle.pass();
        this.reason = vehicle.getReason();
        this.checkTime = vehicle.getCheckTime();
    }

    public Vehicle getVehicle() { return vehicle; }
    public boolean isAllowed() { return allowed; }
    public String getReason() { return reason; }
    public int getCheckTime() { return checkTime; }
}
