package model;


/** Неизменяемая запись истории моделирования. */
public final class Event {
    private final int time, point;
    private final String type, number;

    public Event(int time, String type, Vehicle vehicle, int point) {
        this.time = time;
        this.type = type;
        this.point = point;
        number = vehicle == null ? null : vehicle.getNumber();
    }

    @Override
    public String toString() {
        return "[" + time + " мин] " + type + (number == null ? "" : ": " + number) + (point == 0 ? "" : " (пункт "
                + point + ")");
    }
}
