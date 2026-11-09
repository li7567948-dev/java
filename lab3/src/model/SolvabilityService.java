package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Searches for a practical way to finish vehicle inspections before a deadline.
 *
 * <p>Only genuine changes are allowed: reorder an existing queue or open an
 * installed reserve checkpoint. Every reported positive result is checked by
 * the original lab No. 2 Simulation, never by a made-up time estimate.</p>
 *
 * <p>The tested orderings (original, shortest first, longest first) are
 * heuristics, not an exhaustive proof that no other schedule exists.</p>
 */
public final class SolvabilityService {
    private static final Comparator<Vehicle> SHORTEST_FIRST = new Comparator<Vehicle>() {
        @Override public int compare(Vehicle a, Vehicle b) {
            return Integer.compare(a.getCheckTime(), b.getCheckTime());
        }
    };
    private static final Comparator<Vehicle> LONGEST_FIRST = new Comparator<Vehicle>() {
        @Override public int compare(Vehicle a, Vehicle b) {
            return Integer.compare(b.getCheckTime(), a.getCheckTime());
        }
    };

    public Result analyze(List<Vehicle> vehicles, int activePoints,
                          int installedPoints, int deadlineMinutes) {
        if (vehicles == null || vehicles.contains(null) || activePoints < 1
                || installedPoints < activePoints || installedPoints > 100
                || deadlineMinutes < 1) {
            throw new IllegalArgumentException("Неверные параметры режима разрешимости");
        }

        List<Vehicle> shortestFirst = new ArrayList<Vehicle>(vehicles);
        List<Vehicle> longestFirst = new ArrayList<Vehicle>(vehicles);
        Collections.sort(shortestFirst, SHORTEST_FIRST);
        Collections.sort(longestFirst, LONGEST_FIRST);

        int original = simulate(vehicles, activePoints);
        if (original <= deadlineMinutes) {
            return new Result(original, original, activePoints, deadlineMinutes,
                    true, "Исходный порядок подходит: изменений не требуется.");
        }

        int best = original;
        int bestPointCount = activePoints;
        // First try reordering without opening a reserve checkpoint; then
        // check all existing reserves in increasing order of additional lanes.
        for (int points = activePoints; points <= installedPoints; ++points) {
            List<List<Vehicle>> options = new ArrayList<List<Vehicle>>();
            if (points != activePoints) options.add(vehicles);
            options.add(shortestFirst);
            options.add(longestFirst);

            for (int option = 0; option < options.size(); ++option) {
                int resultMinutes = simulate(options.get(option), points);
                if (resultMinutes < best) {
                    best = resultMinutes;
                    bestPointCount = points;
                }
                if (resultMinutes > deadlineMinutes) continue;
                String change = points == activePoints ? "" :
                        "открыть " + (points - activePoints) + " резервных пунктов";
                if (points == activePoints || option != 0) {
                    String ordering = points == activePoints
                            ? (option == 0 ? "по возрастанию" : "по убыванию")
                            : (option == 1 ? "по возрастанию" : "по убыванию");
                    change += (change.isEmpty() ? "" : " и ")
                            + "изменить порядок очереди " + ordering + " времени проверки";
                }
                return new Result(original, resultMinutes, points, deadlineMinutes,
                        true, "Решение: " + change + ". Проверено исходной моделью Simulation.");
            }
        }
        return new Result(original, best, bestPointCount, deadlineMinutes, false,
                "Решение в проверенных вариантах не найдено: рассмотрены исходный порядок, "
                        + "проверки от коротких к долгим и от долгих к коротким, а также все "
                        + "существующие резервные пункты. Это НЕ доказательство "
                        + "невозможности других перестановок.");
    }

    /** Uses the real lab No. 2 event-driven simulation, minute by minute. */
    public int simulate(List<Vehicle> vehicles, int pointCount) {
        if (vehicles == null || vehicles.contains(null) || pointCount < 1 || pointCount > 100) {
            throw new IllegalArgumentException("Неверные параметры расчёта");
        }
        if (vehicles.isEmpty()) return 0;
        Simulation simulation = new Simulation(pointCount);
        long maximumMinutes = 0;
        for (Vehicle vehicle : vehicles) {
            // Simulation mutates arrival time and rejection reason. Use a copy
            // so asking for a feasibility estimate never changes the caller's queue.
            simulation.addVehicle(VehicleFactory.copyOf(vehicle));
            maximumMinutes += vehicle.getCheckTime();
        }
        if (maximumMinutes > 100000) {
            throw new IllegalArgumentException("Слишком большое время для демонстрации");
        }
        while (simulation.getCompleted() < vehicles.size()) simulation.step();
        return simulation.getTime();
    }

    /** Four legal trailers, each with a four-minute check, demonstrate a reserve lane. */
    public List<Vehicle> exampleQueue() {
        List<Vehicle> vehicles = new ArrayList<Vehicle>();
        for (int index = 0; index < 4; ++index) {
            vehicles.add(new Trailer("ДЕМО-" + (index + 1), 18.0, 2.5, 3.8,
                    30000, 20, Vehicle.SAFE));
        }
        return vehicles;
    }

    /** Recorded conclusion; false means "not found", not "mathematically impossible". */
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
