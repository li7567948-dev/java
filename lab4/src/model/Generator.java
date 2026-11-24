package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Генератор случайного потока автомобилей. */
public final class Generator {
    private final Random random;
    private final int minInterval, maxInterval;

    private long nextArrival;

    private int numberCounter;

    public Generator(int minimum, int maximum) {
        this(minimum, maximum, new Random());
    }

    public Generator(int minimum, int maximum, Random random) {
        if (minimum < 1 || maximum < minimum || maximum == Integer.MAX_VALUE || random == null) {
            throw new IllegalArgumentException("Неверный интервал прибытия");
        }
        minInterval = minimum;
        maxInterval = maximum;
        this.random = random;
    }

    private int integer(int low, int high) {
        return low + random.nextInt(high - low + 1);
    }

    private double decimal(double low, double high) {
        return Math.round((low + random.nextDouble() * (high - low)) * 10) / 10.0;
    }

    public List<Vehicle> newCars(int minute) {
        if (minute < 0) {
            throw new IllegalArgumentException("Отрицательное время");
        }
        List<Vehicle> arrivals = new ArrayList<>();
        while (nextArrival <= minute) {
            arrivals.add(randomCar());
            nextArrival += integer(minInterval, maxInterval);
        }
        return arrivals;
    }

    public Vehicle randomCar() {
        String number = "A" + (++numberCounter) + "BC";
        String[] types = Vehicle.cargoTypes();
        String cargo = random.nextInt(4) == 0 ? types[integer(0, types.length - 1)] : Vehicle.SAFE;
        switch (integer(1, 5)) {
            case 1:
                return new Car(number, decimal(3.5, 5.1), decimal(1.6, 2), decimal(1.4, 2), decimal(1000,
                        3600), integer(20, 62), cargo);
            case 2:
                return new Truck(number, decimal(6, 12.2), decimal(2.2, 2.55), decimal(2.8, 4.1), decimal(5000,
                        41000), integer(10, 72), cargo);
            case 3:
                return new Bus(number, decimal(8, 15), decimal(2.3, 2.55), decimal(2.8, 4), decimal(8000,
                        18500), integer(15, 62), cargo);
            case 4:
                return new Trailer(number, decimal(14, 20.3), decimal(2.45, 2.55), decimal(3.5, 4.05),
                        decimal(15000, 45000), integer(5, 32), cargo);
            default:
                return new CarWithTrailer(number, decimal(6, 10.2), decimal(1.8, 2.2), decimal(1.5,
                        3), decimal(2000, 5200), integer(15, 42), cargo);
        }
    }
}
