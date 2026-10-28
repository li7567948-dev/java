package model;

public class Event {
    private int time;
    private String type;
    private Vehicle vehicle;
    private int point;

    public Event(int time, String type, Vehicle vehicle, int point) {
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
        String text = "[" + time + " мин] " + type;
        if (vehicle != null) text += ": " + vehicle.getNumber();
        if (point > 0) text += " (пункт " + point + ")";
        return text;
    }
}