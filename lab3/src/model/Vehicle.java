package model;

import java.util.Objects;

/**
 * Base vehicle of laboratory work No. 2.
 *
 * <p>Dimensions are in metres, mass in kilograms and speed in km/h.
 * Vehicle-specific restrictions are implemented by subclasses.</p>
 */
public abstract class Vehicle implements CargoCheck {
    public static final String SAFE = "безопасный";
    public static final String FIRE = "огнеопасный";
    public static final String CHEMICAL = "химически опасный";
    public static final String BOOM = "взрывоопасный";
    public static final String SPECIAL = "особой важности";

    public static final String[] CARGO = {SAFE, FIRE, CHEMICAL, BOOM, SPECIAL};

    private final String number;
    private final double length;
    private final double width;
    private final double height;
    private final double weight;
    private final int speed;
    private final String cargo;
    private int arriveTime;
    protected String reason = "";

    protected Vehicle(String number, double length, double width, double height,
                      double weight, int speed, String cargo) {
        if (number == null || number.trim().isEmpty()
                || !Double.isFinite(length) || length <= 0
                || !Double.isFinite(width) || width <= 0
                || !Double.isFinite(height) || height <= 0
                || !Double.isFinite(weight) || weight <= 0 || speed < 0) {
            throw new IllegalArgumentException("Неверные параметры машины");
        }
        if (!isValidCargo(cargo)) {
            throw new IllegalArgumentException("Неизвестный тип груза");
        }
        this.number = number.trim();
        this.length = length;
        this.width = width;
        this.height = height;
        this.weight = weight;
        this.speed = speed;
        this.cargo = cargo;
    }

    public static boolean isValidCargo(String cargo) {
        for (String type : CARGO) {
            if (type.equals(cargo)) {
                return true;
            }
        }
        return false;
    }

    public String getNumber() { return number; }
    public double getLength() { return length; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getWeight() { return weight; }
    public int getSpeed() { return speed; }
    public String getCargo() { return cargo; }
    public String getReason() { return reason; }
    public int getArriveTime() { return arriveTime; }

    public void setArriveTime(int arriveTime) {
        if (arriveTime < 0) {
            throw new IllegalArgumentException("Отрицательное время прибытия");
        }
        this.arriveTime = arriveTime;
    }

    public abstract String getType();
    public abstract double getMaxLength();
    public abstract double getMaxWidth();
    public abstract double getMaxHeight();
    public abstract double getMaxWeight();
    public abstract int getMaxSpeed();

    @Override
    public boolean isDangerous() {
        return FIRE.equals(cargo) || CHEMICAL.equals(cargo) || BOOM.equals(cargo);
    }

    /** Duration of inspection in simulated minutes (identical to lab No. 2). */
    public int getCheckTime() {
        int minutes = length <= 5 ? 1 : length <= 10 ? 2 : length <= 15 ? 3 : 4;
        if (FIRE.equals(cargo) || SPECIAL.equals(cargo)) minutes += 2;
        if (CHEMICAL.equals(cargo)) minutes += 3;
        if (BOOM.equals(cargo)) minutes += 5;
        return minutes;
    }

    /**
     * Checks the same limits as in lab No. 2.
     * The reason is reset on every call, preventing stale rejection messages.
     */
    public boolean pass() {
        StringBuilder violations = new StringBuilder();
        if (length > getMaxLength()) violations.append("длина больше нормы; ");
        if (width > getMaxWidth()) violations.append("ширина больше нормы; ");
        if (height > getMaxHeight()) violations.append("высота больше нормы; ");
        if (weight > getMaxWeight()) violations.append("масса больше нормы; ");
        int allowedSpeed = isDangerous() ? Math.min(getMaxSpeed(), 20) : getMaxSpeed();
        if (speed > allowedSpeed) violations.append("скорость больше нормы; ");
        reason = violations.toString();
        return reason.isEmpty();
    }

    /** Subclasses append their own rules after the common checks. */
    protected final boolean rejectIf(boolean condition, String message, boolean passed) {
        if (condition) {
            reason += message + "; ";
            return false;
        }
        return passed;
    }

    @Override
    public String toString() {
        return getType() + " " + number + ": " + length + "x" + width + "x" + height
                + " м, " + weight + " кг, " + speed + " км/ч, груз " + cargo;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Vehicle)) return false;
        Vehicle that = (Vehicle) other;
        return number.equals(that.number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }
}
