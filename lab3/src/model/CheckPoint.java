package model;

import java.util.Objects;

/** One service lane. A closed or busy lane cannot accept a new vehicle. */
public final class CheckPoint {
    private final int id;
    private boolean open;
    private Vehicle vehicle;
    private int timeLeft;
    private int count;

    public CheckPoint(int id) {
        if (id <= 0) throw new IllegalArgumentException("Неверный номер пункта");
        this.id = id;
    }

    public int getId() { return id; }
    public boolean isOpen() { return open; }
    /** Prevent a busy checkpoint from being closed mid-inspection. */
    public void setOpen(boolean open) {
        if (!open && !isFree()) {
            throw new IllegalStateException("Нельзя закрыть пункт во время проверки");
        }
        this.open = open;
    }
    public boolean isFree() { return vehicle == null; }
    public Vehicle getCurrentVehicle() { return vehicle; }
    public int getTimeLeft() { return timeLeft; }
    public int getCount() { return count; }

    public void take(Vehicle nextVehicle) {
        Objects.requireNonNull(nextVehicle, "Машина отсутствует");
        if (!open || !isFree()) {
            throw new IllegalStateException("Пункт закрыт или занят");
        }
        vehicle = nextVehicle;
        timeLeft = nextVehicle.getCheckTime();
    }

    /** Performs one minute of inspection; null means the vehicle is still busy. */
    public Vehicle work() {
        if (!open || vehicle == null) return null;
        --timeLeft;
        if (timeLeft > 0) return null;
        Vehicle finished = vehicle;
        vehicle = null;
        ++count;
        return finished;
    }
}
