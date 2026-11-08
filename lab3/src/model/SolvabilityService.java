package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Checks whether the vehicles already waiting in a queue can be INSPECTED
 * before a deadline. Only physical remedies are considered: change queue
 * order and open one of the EXISTING reserve checkpoints.
 *
 * <p>Inspection throughput is not a promise that every vehicle will be
 * allowed to cross: admission rules remain those of Vehicle.pass().</p>
 */
public final class SolvabilityService {

    public Result analyze(List<Vehicle> vehicles, int activePoints,
                          int installedPoints, int deadlineMinutes) {
        if (vehicles == null || vehicles.contains(null) || activePoints < 1
                || installedPoints < activePoints || installedPoints > 100
                || deadlineMinutes < 1) {
            throw new IllegalArgumentException("Неверные параметры режима разрешимости");
        }
        int current = simulate(vehicles, activePoints);
        if (current <= deadlineMinutes) {
            return new Result(current, current, activePoints, deadlineMinutes,
                    true, "Исходный порядок подходит: изменений не требуется.");
        }

        List<Vehicle> shortFirst = new ArrayList<Vehicle>(vehicles);
        Collections.sort(shortFirst, new Comparator<Vehicle>() {
            @Override public int compare(Vehicle first, Vehicle second) {
                return Integer.compare(first.getCheckTime(), second.getCheckTime());
            }
        });

        int reordered = simulate(shortFirst, activePoints);
        if (reordered <= deadlineMinutes) {
            return new Result(current, reordered, activePoints, deadlineMinutes,
                    true, "Решение: изменить порядок очереди (сначала короткие проверки). "
                            + "Результат подтверждён моделированием.");
        }

        for (int pointCount = activePoints + 1; pointCount <= installedPoints; ++pointCount) {
            int fifo = simulate(vehicles, pointCount);
            if (fifo <= deadlineMinutes) {
                return new Result(current, fifo, pointCount, deadlineMinutes,
                        true, "Решение: открыть " + (pointCount - activePoints)
                                + " резервных пунктов из существующих " + installedPoints + ".");
            }
            int optimized = simulate(shortFirst, pointCount);
            if (optimized <= deadlineMinutes) {
                return new Result(current, optimized, pointCount, deadlineMinutes,
                        true, "Решение: открыть " + (pointCount - activePoints)
                                + " резервных пунктов и изменить порядок очереди.");
            }
        }
        return new Result(current, current, activePoints, deadlineMinutes, false,
                "В заданный срок решение не найдено: проверены перестановка очереди "
                        + "и все доступные резервные пункты. Физические ограничения не изменяются.");
    }

    /**
     * Runs the actual laboratory work No. 2 event-driven simulation to
     * completion. All vehicles arrive at simulation minute 0.
     */
    public int simulate(List<Vehicle> vehicles, int pointCount) {
        if (vehicles == null || vehicles.contains(null) || pointCount < 1 || pointCount > 100) {
            throw new IllegalArgumentException("Неверные параметры расчёта");
        }
        if (vehicles.isEmpty()) return 0;
        Simulation simulation = new Simulation(pointCount);
        long upperBound = 0;
        for (Vehicle vehicle : vehicles) {
            simulation.addVehicle(vehicle);
            upperBound += vehicle.getCheckTime();
        }
        if (upperBound > 100000) {
            throw new IllegalArgumentException("Слишком большое время для демонстрации");
        }
        while (simulation.getCompleted() < vehicles.size()) simulation.step();
        return simulation.getTime();
    }

    /** Example: 4 trailers (4 min each), one active and one available reserve lane. */
    public List<Vehicle> exampleQueue() {
        List<Vehicle> vehicles = new ArrayList<Vehicle>();
        for (int index = 0; index < 4; ++index) {
            vehicles.add(new Trailer("ДЕМО-" + (index + 1), 18.0, 2.5, 3.8,
                    30000, 20, Vehicle.SAFE));
        }
        return vehicles;
    }

    /** Value object describing an independently verifiable proposal. */
    public static final class Result {
        private final int initialMinutes;
        private final int proposedMinutes;
        private final int proposedPoints;
        private final int deadlineMinutes;
        private final boolean feasible;
        private final String explanation;

        private Result(int initialMinutes, int proposedMinutes, int proposedPoints,
                       int deadlineMinutes, boolean feasible, String explanation) {
            this.initialMinutes = initialMinutes;
            this.proposedMinutes = proposedMinutes;
            this.proposedPoints = proposedPoints;
            this.deadlineMinutes = deadlineMinutes;
            this.feasible = feasible;
            this.explanation = explanation;
        }

        public int getInitialMinutes() { return initialMinutes; }
        public int getProposedMinutes() { return proposedMinutes; }
        public int getProposedPoints() { return proposedPoints; }
        public int getDeadlineMinutes() { return deadlineMinutes; }
        public boolean isFeasible() { return feasible; }
        public String getExplanation() { return explanation; }
    }
}
