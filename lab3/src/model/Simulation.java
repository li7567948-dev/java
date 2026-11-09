package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

/**
 * Root of the class hierarchy used in laboratory work No. 2.
 * Holds checkpoints, vehicles, and the event history.
 * This class is the generic element type of both collections in lab No. 3.
 */
public final class Simulation {
    private final int experimentId;
    private final List<CheckPoint> points = new ArrayList<CheckPoint>();
    private final Queue<Vehicle> queue = new ArrayDeque<Vehicle>();
    private final List<Event> events = new ArrayList<Event>();
    private int time;
    private int completed;
    private int allowed;
    private int denied;

    /** Original lab No. 2 constructor, preserved for existing code. */
    public Simulation(int pointCount) {
        this(0, pointCount);
    }

    /** Identifier is used when a Simulation is stored in a lab No. 3 collection. */
    public Simulation(int experimentId, int pointCount) {
        if (experimentId < 0) {
            throw new IllegalArgumentException("Отрицательный идентификатор опыта");
        }
        this.experimentId = experimentId;
        if (pointCount < 1 || pointCount > 100) {
            throw new IllegalArgumentException("Количество пунктов должно быть от 1 до 100");
        }
        for (int id = 1; id <= pointCount; ++id) {
            CheckPoint point = new CheckPoint(id);
            point.setOpen(true);
            points.add(point);
        }
    }

    public void setPointOpen(int id, boolean open) {
        if (id < 1 || id > points.size()) {
            throw new IllegalArgumentException("Неверный номер пункта");
        }
        CheckPoint point = points.get(id - 1);
        point.setOpen(open);
        events.add(new Event(time, open ? "Пункт открыт" : "Пункт закрыт", null, id));
    }

    public void addVehicle(Vehicle vehicle) {
        if (vehicle == null) throw new IllegalArgumentException("Машина отсутствует");
        vehicle.setArriveTime(time);
        queue.add(vehicle);
        events.add(new Event(time, "Автомобиль прибыл", vehicle, 0));
    }

    public void step() {
        for (CheckPoint point : points) {
            if (point.isOpen() && point.isFree() && !queue.isEmpty()) {
                Vehicle vehicle = queue.remove();
                point.take(vehicle);
                events.add(new Event(time, "Начало проверки", vehicle, point.getId()));
            }
        }
        ++time;
        for (CheckPoint point : points) {
            Vehicle finished = point.work();
            if (finished == null) continue;
            ++completed;
            CheckResult result = new CheckResult(finished);
            if (result.isAllowed()) {
                ++allowed;
                events.add(new Event(time, "Проверка завершена: разрешено", finished, point.getId()));
            } else {
                ++denied;
                events.add(new Event(time,
                        "Проверка завершена: запрещено (" + result.getReason() + ")",
                        finished, point.getId()));
            }
        }
    }

    public void run(int duration) {
        if (duration < 0 || duration > 100000) {
            throw new IllegalArgumentException("Время моделирования должно быть от 0 до 100000");
        }
        for (int minute = 0; minute < duration; ++minute) step();
    }

    public void runReady(int duration) {
        Vehicle[] ready = VehicleFactory.readyVehicles();
        for (int i = 0; i < 3; ++i) addVehicle(ready[i]);
        run(duration);
    }

    public void runGenerated(int duration, int minInterval, int maxInterval) {
        if (duration < 0 || duration > 100000) {
            throw new IllegalArgumentException("Недопустимое время моделирования");
        }
        Generator generator = new Generator(minInterval, maxInterval);
        for (int minute = 0; minute < duration; ++minute) {
            for (Vehicle vehicle : generator.newCars(time)) addVehicle(vehicle);
            step();
        }
    }

    public int getExperimentId() { return experimentId; }
    public int getTime() { return time; }
    public int getCompleted() { return completed; }
    public int getAllowed() { return allowed; }
    public int getDenied() { return denied; }
    public int getQueueSize() { return queue.size(); }
    public List<Vehicle> getQueueSnapshot() {
        return Collections.unmodifiableList(new ArrayList<Vehicle>(queue));
    }
    public List<CheckPoint> getPoints() { return Collections.unmodifiableList(points); }
    public int getActiveCount() {
        int busy = 0;
        for (CheckPoint point : points) if (!point.isFree()) ++busy;
        return busy;
    }
    public List<Event> getEvents() { return Collections.unmodifiableList(events); }
}
