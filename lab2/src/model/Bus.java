package model;

public class Bus extends Vehicle {
    public Bus(String number, double length, double width, double height,
               double weight, int speed, String cargo) {
        super(number, length, width, height, weight, speed, cargo);
    }
    @Override public String getType() { return "Автобус"; }
    @Override public double getMaxWeight() { return 18000; }
    @Override public double getMaxLength() { return 15.0; }
    @Override public double getMaxWidth() { return 2.55; }
    @Override public double getMaxHeight() { return 4.0; }
    @Override public int getMaxSpeed() { return 60; }

    @Override
    public boolean pass() {
        boolean ok = super.pass();
        if (!getCargo().equals(SAFE)) {
            reason += "В автобусе разрешён только безопасный груз; ";
            ok = false;
        }
        return ok;
    }
}