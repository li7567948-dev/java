package model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Driver of a vehicle; checks the required category. */
public final class Driver {
    private final String name;
    private final int experienceYears;
    private final EnumSet<VehicleType> permittedTypes;

    public Driver(String name, int experienceYears, Set<VehicleType> permittedTypes) {
        if (name == null || name.trim().isEmpty() || experienceYears < 0
                || permittedTypes == null || permittedTypes.isEmpty()) {
            throw new IllegalArgumentException("Некорректные данные водителя");
        }
        this.name = name;
        this.experienceYears = experienceYears;
        this.permittedTypes = EnumSet.copyOf(permittedTypes);
    }

    public String getName() { return name; }
    public int getExperienceYears() { return experienceYears; }
    public Set<VehicleType> getPermittedTypes() {
        return Collections.unmodifiableSet(permittedTypes);
    }
    public boolean canDrive(VehicleType type) { return permittedTypes.contains(type); }
}
