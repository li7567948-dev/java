package model;

public class VehicleFactory {
    private VehicleFactory() { }

    public static Vehicle create(int type, String number, double length, double width,
            double height, double weight, int speed, String cargo) {
        switch (type) {
            case 1: return new Car(number, length, width, height, weight, speed, cargo);
            case 2: return new Truck(number, length, width, height, weight, speed, cargo);
            case 3: return new Bus(number, length, width, height, weight, speed, cargo);
            case 4: return new Trailer(number, length, width, height, weight, speed, cargo);
            case 5: return new CarWithTrailer(number, length, width, height, weight, speed, cargo);
            default: throw new IllegalArgumentException("Неизвестный тип машины");
        }
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