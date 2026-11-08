package model;

/** Car towing a trailer: only safe or flammable cargo is permitted. */
public final class CarWithTrailer extends Vehicle {
    public CarWithTrailer(String number, double length, double width, double height,
                          double weight, int speed, String cargo) {
        super(number, length, width, height, weight, speed, cargo);
    }

    @Override
    public String getType() {
        return "Легковой автомобиль с прицепом";
    }
    @Override
    public double getMaxWeight() {
        return 5000;
    }
    @Override
    public double getMaxLength() {
        return 10.0;
    }
    @Override
    public double getMaxWidth() {
        return 2.2;
    }
    @Override
    public double getMaxHeight() {
        return 3.0;
    }
    @Override
    public int getMaxSpeed() {
        return 40;
    }

    @Override
    public boolean pass() {
        boolean passed = super.pass();
        return rejectIf(!SAFE.equals(getCargo()) && !FIRE.equals(getCargo()),
                "Недопустимый тип груза для автомобиля с прицепом", passed);
    }
}
