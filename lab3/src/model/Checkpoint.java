package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Main class: contains a hierarchy of Vehicle objects and inspection lanes. */
public final class Checkpoint {
    private final String name;
    private final int limitMinutes;
    private final List<Vehicle> vehicles;
    private final List<InspectionLane> lanes;

    public Checkpoint(String name, int limitMinutes, List<Vehicle> vehicles,
                      List<InspectionLane> lanes) {
        if (name == null || name.trim().isEmpty() || limitMinutes <= 0
                || vehicles == null || lanes == null || lanes.isEmpty()) {
            throw new IllegalArgumentException("Некорректные параметры пункта пропуска");
        }
        this.name = name;
        this.limitMinutes = limitMinutes;
        this.vehicles = Collections.unmodifiableList(new ArrayList<Vehicle>(vehicles));
        this.lanes = Collections.unmodifiableList(new ArrayList<InspectionLane>(lanes));
        if (this.vehicles.contains(null) || this.lanes.contains(null)) {
            throw new IllegalArgumentException("Коллекция содержит null");
        }
    }

    public String getName() { return name; }
    public int getLimitMinutes() { return limitMinutes; }
    public List<Vehicle> getVehicles() { return vehicles; }
    public List<InspectionLane> getLanes() { return lanes; }
}
