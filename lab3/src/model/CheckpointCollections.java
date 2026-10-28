package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/** Task 1: BOTH collections hold the SAME principal type: Checkpoint. */
public final class CheckpointCollections {
    private final ArrayList<Checkpoint> arrayCheckpoints = new ArrayList<Checkpoint>();
    private final HashMap<Integer, Checkpoint> mapCheckpoints = new HashMap<Integer, Checkpoint>();

    public void fill(CheckpointGenerator generator, int count) {
        if (count < 0) throw new IllegalArgumentException("Отрицательный размер");
        arrayCheckpoints.clear();
        mapCheckpoints.clear();
        for (int index = 0; index < count; index++) {
            Checkpoint object = generator.generateCheckpoint(index + 1, 3);
            arrayCheckpoints.add(object);
            mapCheckpoints.put(index, object);
        }
    }

    public List<Checkpoint> getArrayCheckpoints() { return arrayCheckpoints; }
    public Map<Integer, Checkpoint> getMapCheckpoints() { return mapCheckpoints; }
}
