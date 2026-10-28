package model;

import java.util.Objects;

/** Base class for the lab №2 checkpoint vehicle hierarchy. */
public abstract class Vehicle {
    public static final String SAFE = "безопасный";
    public static final String NEEDS_INSPECTION = "требует дополнительной проверки";

    private final String registration;
    private final Driver driver;
    private final boolean extraInspection;

    protected Vehicle(String registration, Driver driver, boolean extraInspection) {
        if (registration == null || registration.trim().isEmpty()) {
            throw new IllegalArgumentException("Номер транспортного средства пуст");
        }
        this.registration = registration;
        this.driver = Objects.requireNonNull(driver, "Водитель отсутствует");
        this.extraInspection = extraInspection;
    }

    public abstract VehicleType getType();
    public String getRegistration() { return registration; }
    public Driver getDriver() { return driver; }
    public boolean needsExtraInspection() { return extraInspection; }
    public int getInspectionMinutes() {
        return getType().getBaseMinutes() + (extraInspection ? 4 : 0);
    }
    public boolean driverIsQualified() { return driver.canDrive(getType()); }
    public String getSafetyStatus() {
        return extraInspection ? NEEDS_INSPECTION : SAFE;
    }
    @Override public String toString() {
        return getType().getLabel() + " [" + registration + ", " + getSafetyStatus()
                + ", " + getInspectionMinutes() + " мин]";
    }
}
