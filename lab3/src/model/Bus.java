package model;

/** Bus: only safe cargo may be carried (laboratory work No. 2). */
public final class Bus extends Vehicle {
    public Bus(String number, double length, double width, double height,
               double weight, int speed, String cargo) {
        super(number, length, width, height, weight, speed, cargo);
    }

    @Override
    public String getType() {
        return "Автобус";
    }
    @Override
    public double getMaxWeight() {
        return 18000;
    }
    @Override
    public double getMaxLength() {
        return 15.0;
    }
    @Override
    public double getMaxWidth() {
        return 2.55;
    }
    @Override
    public double getMaxHeight() {
        return 4.0;
    }
    @Override
    public int getMaxSpeed() {
        return 60;
    }

    @Override
    public boolean pass() {
        boolean passed = super.pass();
        return rejectIf(!SAFE.equals(getCargo()),
                "В автобусе разрешён только безопасный груз", passed);
    }
}
