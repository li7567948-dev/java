package model;

/** Creates the same five classes of vehicle as laboratory work No. 2. */
public final class VehicleFactory {
    private VehicleFactory() { }

    public static Vehicle create(int type, String number, double length, double width,
                                 double height, double weight, int speed, String cargo) {
        switch (type) {
            case 1: return new Car(number, length, width, height, weight, speed, cargo);
            case 2: return new Truck(number, length, width, height, weight, speed, cargo);
            case 3: return new Bus(number, length, width, height, weight, speed, cargo);
            case 4: return new Trailer(number, length, width, height, weight, speed, cargo);
            case 5: return new CarWithTrailer(number, length, width, height, weight, speed, cargo);
            default: throw new IllegalArgumentException("Неизвестный тип машины: " + type);
        }
    }

    /** Creates an independent copy, retaining the lab No. 2 vehicle subclass. */
    public static Vehicle copyOf(Vehicle original) {
        if (original == null) throw new IllegalArgumentException("Машина отсутствует");
        final int type;
        if (original instanceof Car) {
            type = 1;
        } else if (original instanceof Truck) {
            type = 2;
        } else if (original instanceof Bus) {
            type = 3;
        } else if (original instanceof Trailer) {
            type = 4;
        } else if (original instanceof CarWithTrailer) {
            type = 5;
        } else {
            throw new IllegalArgumentException("Неизвестный тип машины");
        }
        return create(type, original.getNumber(), original.getLength(), original.getWidth(),
                original.getHeight(), original.getWeight(), original.getSpeed(),
                original.getCargo());
    }

    public static Vehicle[] readyVehicles() {
        return new Vehicle[] {
                create(1, "A001", 4.5, 1.8, 1.5, 1500, 60, Vehicle.SAFE),
                create(2, "B002", 10.0, 2.4, 3.5, 20000, 50, Vehicle.FIRE),
                create(3, "C003", 12.0, 2.5, 3.5, 12000, 50, Vehicle.SAFE),
                create(4, "D004", 18.0, 2.5, 3.8, 30000, 30, Vehicle.CHEMICAL),
                create(5, "E005", 8.0, 2.0, 2.5, 3000, 35, Vehicle.SAFE)
        };
    }
}
