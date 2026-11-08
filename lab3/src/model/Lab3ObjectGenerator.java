package model;

import java.util.Random;

/**
 * Random test objects for laboratory work No. 3, including the root class
 * and the classes directly related to it (requirement 7.1).
 *
 * <p>A fixed seed makes experiments reproducible. No I/O happens here.</p>
 */
public final class Lab3ObjectGenerator {
    private final Random random;
    private long nextVehicleNumber;

    public Lab3ObjectGenerator(long seed) {
        this.random = new Random(seed);
    }

    public Car generateCar() {
        return new Car(nextNumber(), betweenDecimal(3.7, 6.2),
                betweenDecimal(1.7, 2.6), betweenDecimal(1.4, 3.1),
                between(1200, 3600), between(20, 95), randomCargo());
    }

    public Truck generateTruck() {
        return new Truck(nextNumber(), betweenDecimal(6.0, 12.4),
                betweenDecimal(2.2, 2.55), betweenDecimal(2.8, 4.1),
                between(12000, 41000), between(10, 75), randomCargo());
    }

    public Bus generateBus() {
        return new Bus(nextNumber(), betweenDecimal(8.0, 15.2),
                betweenDecimal(2.3, 2.55), betweenDecimal(2.8, 4.1),
                between(9000, 19000), between(15, 65), randomCargo());
    }

    public Trailer generateTrailer() {
        return new Trailer(nextNumber(), betweenDecimal(14.0, 20.3),
                betweenDecimal(2.45, 2.6), betweenDecimal(3.5, 4.1),
                between(17000, 45000), between(5, 35), randomCargo());
    }

    public CarWithTrailer generateCarWithTrailer() {
        return new CarWithTrailer(nextNumber(), betweenDecimal(6.0, 10.3),
                betweenDecimal(1.8, 2.3), betweenDecimal(1.5, 3.1),
                between(2600, 5300), between(10, 45), randomCargo());
    }

    public Vehicle generateVehicle() {
        switch (random.nextInt(5)) {
            case 0: return generateCar();
            case 1: return generateTruck();
            case 2: return generateBus();
            case 3: return generateTrailer();
            default: return generateCarWithTrailer();
        }
    }

    public CheckPoint generateCheckPoint(int id) {
        CheckPoint point = new CheckPoint(id);
        point.setOpen(true);
        return point;
    }

    public Event generateEvent(Vehicle vehicle, int minute) {
        return new Event(minute, "Автомобиль прибыл", vehicle, 0);
    }

    public CheckResult generateCheckResult(Vehicle vehicle) {
        return new CheckResult(vehicle);
    }

    public Generator generateArrivalGenerator() {
        return new Generator(1, 3, new Random(random.nextLong()));
    }

    /** A meaningful root object with checkpoints, queued vehicles and arrival events. */
    public Simulation generateSimulation(int id) {
        if (id < 0) throw new IllegalArgumentException("Отрицательный индекс");
        Simulation simulation = new Simulation(1 + random.nextInt(3));
        int arrivals = 1 + random.nextInt(2);
        for (int i = 0; i < arrivals; ++i) simulation.addVehicle(generateVehicle());
        return simulation;
    }

    private String nextNumber() {
        return String.format("ЛР3-%08d", ++nextVehicleNumber);
    }

    private int between(int minimum, int maximum) {
        return minimum + random.nextInt(maximum - minimum + 1);
    }

    private double betweenDecimal(double minimum, double maximum) {
        return Math.round((minimum + random.nextDouble() * (maximum - minimum)) * 100.0)
                / 100.0;
    }

    private String randomCargo() {
        return Vehicle.CARGO[random.nextInt(Vehicle.CARGO.length)];
    }
}
