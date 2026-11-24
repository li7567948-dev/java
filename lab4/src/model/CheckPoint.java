package model;


/** Пункт пропуска; занятый пункт нельзя закрыть. */
public final class CheckPoint {
    private final int id;

    private boolean open;

    private Vehicle vehicle;

    private int timeLeft, count;

    public CheckPoint(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Неверный номер пункта");
        }
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isFree() {
        return vehicle == null;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public int getCount() {
        return count;
    }

    public void setOpen(boolean value) {
        if (!value && !isFree()) {
            throw new IllegalStateException("Пункт занят");
        }
        open = value;
    }

    public void take(Vehicle next) {
        if (!open || !isFree() || next == null) {
            throw new IllegalStateException("Пункт закрыт или занят");
        }
        vehicle = next;
        timeLeft = next.getCheckTime();
    }

    public Vehicle work() {
        if (!open || vehicle == null) {
            return null;
        }
        if (--timeLeft > 0) {
            return null;
        }
        Vehicle finished = vehicle;
        vehicle = null;
        count++;
        return finished;
    }
}
