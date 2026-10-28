package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Requirement 7.2: analyze real lane capacity; no zero-duration shortcuts. */
public final class SolvabilityService {
    public static final class Result {
        private final boolean feasible;
        private final int currentMinutes;
        private final String explanation;
        public Result(boolean feasible, int currentMinutes, String explanation) {
            this.feasible = feasible;
            this.currentMinutes = currentMinutes;
            this.explanation = explanation;
        }
        public boolean isFeasible() { return feasible; }
        public int getCurrentMinutes() { return currentMinutes; }
        public String getExplanation() { return explanation; }
    }

    public Result analyze(Checkpoint checkpoint) {
        List<Vehicle> vehicles = checkpoint.getVehicles();
        List<InspectionLane> active = new ArrayList<InspectionLane>();
        List<InspectionLane> reserve = new ArrayList<InspectionLane>();
        for (InspectionLane lane : checkpoint.getLanes()) {
            if (lane.isActive()) active.add(lane);
            else reserve.add(lane);
        }
        if (active.isEmpty()) {
            return new Result(false, -1, "Нет действующих полос контроля.");
        }
        for (Vehicle vehicle : vehicles) {
            if (!vehicle.driverIsQualified()) {
                return new Result(false, -1, "Водитель машины " + vehicle.getRegistration()
                        + " не имеет нужной категории прав. Требуется допущенный водитель.");
            }
        }
        int current = duration(vehicles, active);
        int deadline = checkpoint.getLimitMinutes();
        if (current >= 0 && current <= deadline) {
            return new Result(true, current,
                    "Все автомобили могут пройти контроль за " + current
                            + " мин при лимите " + deadline + " мин.");
        }

        List<Vehicle> reordered = new ArrayList<Vehicle>(vehicles);
        // LPT: priority by inspection time, without changing any physical duration.
        Collections.sort(reordered, Comparator.comparingInt(Vehicle::getInspectionMinutes).reversed());
        int reorderedMinutes = duration(reordered, active);
        if (reorderedMinutes >= 0 && reorderedMinutes <= deadline) {
            return new Result(false, current,
                    "Текущий порядок не укладывается в " + deadline
                            + " мин. Решение: изменить порядок машин по убыванию времени контроля; "
                            + "после перестановки требуется " + reorderedMinutes + " мин.");
        }
        if (!reserve.isEmpty()) {
            List<InspectionLane> additional = new ArrayList<InspectionLane>(active);
            for (InspectionLane lane : reserve) {
                additional.add(lane);
                int withReserve = duration(reordered, additional);
                if (withReserve >= 0 && withReserve <= deadline) {
                    return new Result(false, current,
                            "Текущий план не укладывается в " + deadline
                                    + " мин. Решение: открыть существующую резервную полосу «"
                                    + lane.getName() + "» и упорядочить очередь; "
                                    + "план займёт " + withReserve + " мин.");
                }
            }
        }
        return new Result(false, current,
                "За " + deadline + " мин задача неразрешима при доступных полосах и "
                        + "реальном времени проверки. Необходим больший лимит или дополнительная "
                        + "физически существующая подходящая полоса.");
    }

    /** Earliest-compatible-lane schedule, in the supplied queue order. */
    private int duration(List<Vehicle> vehicles, List<InspectionLane> lanes) {
        int[] finish = new int[lanes.size()];
        for (Vehicle vehicle : vehicles) {
            int index = -1;
            for (int i = 0; i < lanes.size(); i++) {
                if (lanes.get(i).canInspect(vehicle)
                        && (index == -1 || finish[i] < finish[index])) index = i;
            }
            if (index == -1) return -1;
            finish[index] += vehicle.getInspectionMinutes();
        }
        int max = 0;
        for (int value : finish) max = Math.max(max, value);
        return max;
    }
}
