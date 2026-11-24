package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

/** Моделирование очереди; данные исходной коллекции не изменяются. */
public final class Simulation {
    private final List<CheckPoint> points = new ArrayList<>();
    private final Queue<Vehicle> queue = new ArrayDeque<>();
    private final List<Event> events = new ArrayList<>();

    private int time, completed, allowed, denied;

    public Simulation(int count) {
        if (count < 1 || count > 100) {
            throw new IllegalArgumentException("Количество пунктов: 1–100");
        }
        for (int id = 1; id <= count; id++) {
            CheckPoint point = new CheckPoint(id);
            point.setOpen(true);
            points.add(point);
        }
    }

    public void setPointOpen(int id, boolean open) {
        if (id < 1 || id > points.size()) {
            throw new IllegalArgumentException("Неверный номер пункта");
        }
        points.get(id - 1).setOpen(open);
        events.add(new Event(time, open ? "Пункт открыт" : "Пункт закрыт", null, id));
    }

    public void addVehicle(Vehicle vehicle) {
        Vehicle copy = VehicleFactory.copyOf(vehicle);
        copy.setArriveTime(time);
        queue.add(copy);
        events.add(new Event(time, "Автомобиль прибыл", copy, 0));
    }

    /** Распределяет ожидающие машины и выполняет одну минуту проверки. */
    public void step() {
        for (CheckPoint point : points) {
            if (point.isOpen() && point.isFree() && !queue.isEmpty()) {
                Vehicle vehicle = queue.remove();
                point.take(vehicle);
                events.add(new Event(time, "Начало проверки", vehicle, point.getId()));
            }
        }
        time++;
        for (CheckPoint point : points) {
            Vehicle finished = point.work();
            if (finished == null) {
                continue;
            }
            completed++;
            CheckResult result = new CheckResult(finished);
            if (result.isAllowed()) {
                allowed++;
            } else {
                denied++;
            }
            events.add(new Event(time, result.isAllowed() ? "Проверка завершена: разрешено":
                    "Проверка завершена: запрещено (" + result.getReason() + ")", finished, point.getId()));
        }
    }

    private static void validateDuration(int duration) {
        if (duration < 0 || duration > 100000) {
            throw new IllegalArgumentException("Время: 0–100000 минут");
        }
    }

    public void run(int duration) {
        validateDuration(duration);
        for (int minute = 0; minute < duration; minute++) {
            step();
        }
    }

    public void runReady(int duration) {
        validateDuration(duration);
        Vehicle[] ready = VehicleFactory.readyVehicles();
        for (int minute = 0; minute < 3; minute++) {
            addVehicle(ready[minute]);
        }
        run(duration);
    }

    public void runGenerated(int duration, int minInterval, int maxInterval) {
        validateDuration(duration);
        Generator generator = new Generator(minInterval, maxInterval);
        for (int minute = 0; minute < duration; minute++) {
            for (Vehicle vehicle : generator.newCars(minute)) {
                addVehicle(vehicle);
            }
            step();
        }
    }

    public int getTime() {
        return time;
    }

    public int getCompleted() {
        return completed;
    }

    public int getAllowed() {
        return allowed;
    }

    public int getDenied() {
        return denied;
    }

    public int getQueueSize() {
        return queue.size();
    }

    public int getActiveCount() {
        int count = 0;
        for (CheckPoint point : points) {
            if (!point.isFree()) {
                count++;
            }
        }
        return count;
    }

    public List<Event> getEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }
}
