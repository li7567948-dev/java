package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Arrival stream generator from laboratory work No. 2. */
public final class Generator {
    private final Random random;
    private final int minInterval;
    private final int maxInterval;
    private long nextArrival;
    private int numberCounter;

    public Generator(int minInterval, int maxInterval) {
        this(minInterval, maxInterval, new Random());
    }

    public Generator(int minInterval, int maxInterval, Random random) {
        if (minInterval < 1 || maxInterval < minInterval || maxInterval == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Неверный интервал прибытия");
        }
        if (random == null) throw new IllegalArgumentException("Генератор чисел не задан");
        this.minInterval = minInterval;
        this.maxInterval = maxInterval;
        this.random = random;
    }

    public List<Vehicle> newCars(int minute) {
        if (minute < 0) throw new IllegalArgumentException("Отрицательное время");
        List<Vehicle> arrivals = new ArrayList<Vehicle>();
        while (nextArrival <= minute) {
            arrivals.add(randomCar());
            nextArrival += randomInteger(minInterval, maxInterval);
        }
        return arrivals;
    }

    private int randomInteger(int low, int high) {
        return low + random.nextInt(high - low + 1);
    }

    private double randomDouble(double low, double high) {
        return Math.round((low + random.nextDouble() * (high - low)) * 10) / 10.0;
    }

    public Vehicle randomCar() {
        String number = "A" + (++numberCounter) + "BC";
        String cargo = random.nextInt(4) == 0
                ? Vehicle.CARGO[randomInteger(0, Vehicle.CARGO.length - 1)] : Vehicle.SAFE;
        switch (randomInteger(0, 4)) {
            case 0: return new Car(number, randomDouble(3.5, 5.1), randomDouble(1.6, 2.0),
                    randomDouble(1.4, 2.0), randomDouble(1000, 3600), randomInteger(20, 62), cargo);
            case 1: return new Truck(number, randomDouble(6, 12.2), randomDouble(2.2, 2.55),
                    randomDouble(2.8, 4.1), randomDouble(5000, 41000), randomInteger(10, 72), cargo);
            case 2: return new Bus(number, randomDouble(8, 15), randomDouble(2.3, 2.55),
                    randomDouble(2.8, 4), randomDouble(8000, 18500), randomInteger(15, 62), cargo);
            case 3: return new Trailer(number, randomDouble(14, 20.3), randomDouble(2.45, 2.55),
                    randomDouble(3.5, 4.05), randomDouble(15000, 45000), randomInteger(5, 32), cargo);
            default: return new CarWithTrailer(number, randomDouble(6, 10.2), randomDouble(1.8, 2.2),
                    randomDouble(1.5, 3), randomDouble(2000, 5200), randomInteger(15, 42), cargo);
        }
    }
}
