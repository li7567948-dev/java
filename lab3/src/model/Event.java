package model;

/** One immutable simulation event. */
public final class Event {
    private final int time;
    private final String type;
    private final Vehicle vehicle;
    private final int point;

    public Event(int time, String type, Vehicle vehicle, int point) {
        if (time < 0 || type == null || type.trim().isEmpty() || point < 0) {
            throw new IllegalArgumentException("Неверные параметры события");
        }
        this.time = time;
        this.type = type;
        this.vehicle = vehicle;
        this.point = point;
    }

    public int getTime() { return time; }
    public String getType() { return type; }
    public Vehicle getVehicle() { return vehicle; }
    public int getPoint() { return point; }

    @Override
    public String toString() {
        String message = "[" + time + " мин] " + type;
        if (vehicle != null) message += ": " + vehicle.getNumber();
        if (point > 0) message += " (пункт " + point + ")";
        return message;
    }
}
