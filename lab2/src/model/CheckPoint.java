package model;

public class CheckPoint {
    private int id;
    private boolean open;
    private Vehicle car;
    private int timeLeft;
    private int count;

    public CheckPoint(int id) {
        this.id = id;
        this.open = false;
        this.car = null;
        this.timeLeft = 0;
        this.count = 0;
    }

    public int getId() { return id; }
    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }
    public boolean isFree() { return car == null; }
    public int getTimeLeft() { return timeLeft; }
    public int getCount() { return count; }

    public void take(Vehicle v) {
        if (!open || car != null || v == null)
            throw new IllegalStateException("Пункт закрыт или занят");
        car = v;
        timeLeft = v.getCheckTime();
    }

    public Vehicle work() {
        if (!open || car == null) return null;
        timeLeft--;
        if (timeLeft > 0) return null;
        Vehicle done = car;
        car = null;
        count++;
        return done;
    }
}