package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/** Requirement 7.1: generate Checkpoint AND every closely connected model class. */
public final class CheckpointGenerator {
    private final Random random;
    private int nextVehicleId;

    public CheckpointGenerator(long seed) { this.random = new Random(seed); }

    public Driver generateDriver(VehicleType type) {
        // Driver is always legally licensed for the generated vehicle.
        EnumSet<VehicleType> licenses = EnumSet.of(type);
        if (type != VehicleType.PASSENGER) licenses.add(VehicleType.PASSENGER);
        return new Driver("Водитель-" + (1 + random.nextInt(90000)),
                1 + random.nextInt(25), licenses);
    }

    public PassengerCar generatePassengerCar() {
        return new PassengerCar(nextPlate(), generateDriver(VehicleType.PASSENGER),
                random.nextInt(5) == 0);
    }

    public Bus generateBus() {
        return new Bus(nextPlate(), generateDriver(VehicleType.BUS),
                random.nextInt(5) == 0);
    }

    public Truck generateTruck() {
        return new Truck(nextPlate(), generateDriver(VehicleType.TRUCK),
                random.nextInt(5) == 0);
    }

    public Vehicle generateVehicle() {
        int variant = random.nextInt(3);
        if (variant == 0) return generatePassengerCar();
        if (variant == 1) return generateBus();
        return generateTruck();
    }

    public InspectionLane generateInspectionLane(int number, boolean active) {
        // One passenger-only lane and universal lanes; reserve lanes really exist.
        EnumSet<VehicleType> allowed = number == 2
                ? EnumSet.of(VehicleType.PASSENGER)
                : EnumSet.allOf(VehicleType.class);
        return new InspectionLane("Полоса " + number, allowed, active);
    }

    public Checkpoint generateCheckpoint(int id, int vehicleCount) {
        if (vehicleCount < 0) throw new IllegalArgumentException("Отрицательное количество машин");
        List<Vehicle> vehicles = new ArrayList<Vehicle>();
        for (int i = 0; i < vehicleCount; i++) vehicles.add(generateVehicle());
        List<InspectionLane> lanes = Arrays.asList(
                generateInspectionLane(1, true), generateInspectionLane(2, true),
                generateInspectionLane(3, false));
        return new Checkpoint("Пункт пропуска №" + id, 25, vehicles, lanes);
    }

    /** Deterministic case: four 8-min trucks; opening the real reserve lane helps. */
    public Checkpoint generateSolvabilityExample() {
        List<Vehicle> vehicles = new ArrayList<Vehicle>();
        for (int i = 0; i < 4; i++) {
            vehicles.add(new Truck("ТЕСТ-" + (i + 1), generateDriver(VehicleType.TRUCK), false));
        }
        return new Checkpoint("Пример режима разрешимости", 16, vehicles,
                Arrays.asList(generateInspectionLane(1, true),
                        generateInspectionLane(2, true), generateInspectionLane(3, false)));
    }

    private String nextPlate() { return String.format("А%06d", ++nextVehicleId); }
}
