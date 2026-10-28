package model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** A physically existing control lane; a reserve lane can be activated. */
public final class InspectionLane {
    private final String name;
    private final EnumSet<VehicleType> supportedTypes;
    private final boolean active;

    public InspectionLane(String name, Set<VehicleType> supportedTypes, boolean active) {
        if (name == null || name.trim().isEmpty() || supportedTypes == null
                || supportedTypes.isEmpty()) {
            throw new IllegalArgumentException("Некорректная полоса контроля");
        }
        this.name = name;
        this.supportedTypes = EnumSet.copyOf(supportedTypes);
        this.active = active;
    }

    public String getName() { return name; }
    public boolean isActive() { return active; }
    public boolean canInspect(Vehicle vehicle) {
        return supportedTypes.contains(vehicle.getType());
    }
    public Set<VehicleType> getSupportedTypes() {
        return Collections.unmodifiableSet(supportedTypes);
    }
}
