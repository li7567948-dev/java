package model;

import java.util.Objects;

/** Общие свойства и правила проверки транспортных средств (ЛР 2 и 3). */
public abstract class Vehicle implements CargoCheck {

    public static final String SAFE = "безопасный", FIRE = "огнеопасный", CHEMICAL = "химически опасный",
            BOOM = "взрывоопасный", SPECIAL = "особой важности";

    private static final String[] CARGO = {
        SAFE, FIRE, CHEMICAL, BOOM, SPECIAL
    };
    private final String number, cargo;
    private final double length, width, height, weight;
    private final int speed;

    private int arriveTime;

    private String reason = "";

    protected Vehicle(String number, double length, double width, double height, double weight, int speed,
            String cargo) {
        if (number == null || number.trim().isEmpty() || number.matches("(?s).*[\\p{Cntrl}].*") || !positive(length)
                || !positive(width) || !positive(height) || !positive(weight) || speed < 0 || !isValidCargo(cargo)) {
            throw new IllegalArgumentException("Неверные параметры машины");
        }
        this.number = number.trim();
        this.length = length;
        this.width = width;
        this.height = height;
        this.weight = weight;
        this.speed = speed;
        this.cargo = cargo;
    }

    private static boolean positive(double value) {
        return Double.isFinite(value) && value > 0;
    }

    public static String[] cargoTypes() {
        return CARGO.clone();
    }
    static boolean isValidCargo(String value) {
        for (String item : CARGO) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public String getNumber() {
        return number;
    }

    public String getCargo() {
        return cargo;
    }

    public double getLength() {
        return length;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getWeight() {
        return weight;
    }

    public int getSpeed() {
        return speed;
    }

    public int getArriveTime() {
        return arriveTime;
    }

    public void setArriveTime(int time) {
        if (time < 0) {
            throw new IllegalArgumentException("Отрицательное время");
        }
        arriveTime = time;
    }

    public String getReason() {
        return reason;
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

    /** @return длительность проверки в минутах с учётом груза и длины */
    public int getCheckTime() {
        int time = length <= 5 ? 1 : length <= 10 ? 2 : length <= 15 ? 3 : 4;
        if (FIRE.equals(cargo) || SPECIAL.equals(cargo)) {
            time += 2;
        }
        if (CHEMICAL.equals(cargo)) {
            time += 3;
        }
        if (BOOM.equals(cargo)) {
            time += 5;
        }
        return time;
    }

    /** @return true, если все общие ограничения соблюдены; причины отказа пересчитываются */
    public boolean pass() {
        StringBuilder violations = new StringBuilder();
        if (length > getMaxLength()) {
            violations.append("длина больше нормы; ");
        }
        if (width > getMaxWidth()) {
            violations.append("ширина больше нормы; ");
        }
        if (height > getMaxHeight()) {
            violations.append("высота больше нормы; ");
        }
        if (weight > getMaxWeight()) {
            violations.append("масса больше нормы; ");
        }
        int limit = isDangerous() ? Math.min(getMaxSpeed(), 20) : getMaxSpeed();
        if (speed > limit) {
            violations.append("скорость больше нормы; ");
        }
        reason = violations.toString();
        return reason.isEmpty();
    }
    protected final boolean rejectIf(boolean condition, String message, boolean passed) {
        if (condition) {
            reason += message + "; ";
            return false;
        }
        return passed;
    }

    @Override
    public String toString() {
        return getType() + " " + number + ": " + length + "x" + width + "x" + height + " м, " + weight
                + " кг, " + speed + " км/ч, груз " + cargo;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Vehicle && number.equals(((Vehicle) other).number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }
}
