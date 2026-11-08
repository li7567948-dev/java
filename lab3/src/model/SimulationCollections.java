package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Two collection implementations containing the SAME root class, Simulation.
 * Keeping this dedicated class also makes task No. 1 easy to demonstrate.
 */
public final class SimulationCollections {
    private final ArrayList<Simulation> arrayList = new ArrayList<Simulation>();
    private final HashMap<Integer, Simulation> hashMap = new HashMap<Integer, Simulation>();

    public void fill(int size, Lab3ObjectGenerator generator) {
        if (size < 0 || generator == null) {
            throw new IllegalArgumentException("Некорректные параметры коллекции");
        }
        arrayList.clear();
        hashMap.clear();
        for (int id = 0; id < size; ++id) {
            Simulation object = generator.generateSimulation(id);
            arrayList.add(object);
            hashMap.put(id, object);
        }
    }

    public ArrayList<Simulation> getArrayList() { return new ArrayList<Simulation>(arrayList); }
    public Map<Integer, Simulation> getHashMap() { return new HashMap<Integer, Simulation>(hashMap); }
    public int size() { return arrayList.size(); }
    public boolean containsSameObjects() {
        if (arrayList.size() != hashMap.size()) return false;
        for (int id = 0; id < arrayList.size(); ++id) {
            if (arrayList.get(id) != hashMap.get(id)) return false;
        }
        return true;
    }
}
