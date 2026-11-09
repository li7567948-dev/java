package tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import controller.ArrayListCapacityStudy;
import controller.BenchmarkService;
import controller.Controller;
import controller.ErrMsgLog;
import model.Bus;
import model.Car;
import model.CarWithTrailer;
import model.CheckPoint;
import model.CheckResult;
import model.Event;
import model.Generator;
import model.Lab3ObjectGenerator;
import model.Simulation;
import model.SimulationCollections;
import model.SolvabilityService;
import model.Trailer;
import model.Truck;
import model.Vehicle;
import model.VehicleFactory;
import view.ConsoleView;

/** Independent offline tests without JUnit or third-party dependencies. */
public final class Lab3Tests {
    private static int assertions;
    private Lab3Tests() { }

    public static void main(String[] args) throws Exception {
        testVehicleHierarchy();
        testValidation();
        testSimulation();
        testGenerator();
        testCollections();
        testSolvability();
        testInteractiveController();
        testBenchmarks();
        testCapacity();
        if (args.length == 1 && "--full".equals(args[0])) testFullBenchmarks();
        System.out.println("ВСЕ ТЕСТЫ ПРОЙДЕНЫ. Проверок: " + assertions);
    }

    private static void testVehicleHierarchy() {
        Vehicle[] five = VehicleFactory.readyVehicles();
        check(five.length == 5, "There are five vehicle subclasses");
        check(five[0] instanceof Car && five[1] instanceof Truck
                && five[2] instanceof Bus && five[3] instanceof Trailer
                && five[4] instanceof CarWithTrailer, "Five types match lab2");
        check(new CheckResult(five[0]).isAllowed(), "Safe car is allowed");
        check(!new CheckResult(five[1]).isAllowed(), "Dangerous speeding truck is denied");
        check(new CheckResult(five[2]).isAllowed(), "Safe bus is allowed");
        check(five[3].getCheckTime() == 7, "Chemical trailer takes seven minutes");
        check(!new Bus("BUS-1", 10, 2.5, 3, 9000, 20, Vehicle.CHEMICAL).pass(),
                "Bus rejects chemical cargo");
        check(!new CarWithTrailer("TR-1", 7, 2, 2, 4000, 20, Vehicle.BOOM).pass(),
                "Car with trailer rejects explosive cargo");
        check(!new Trailer("LONG-1", 18, 2.5, 3.8, 30000, 15, Vehicle.BOOM).pass(),
                "Explosive trailer exceeds 10 km/h limit");
        check(new Trailer("LONG-2", 18, 2.5, 3.8, 30000, 10, Vehicle.BOOM).pass(),
                "Explosive trailer passes extra speed rule at 10 km/h");
        CheckResult original = new CheckResult(five[1]);
        new CheckResult(five[1]);
        check(original.getReason().equals(five[1].getReason()), "Reason is stable");
        check(new Car("T-1", 4, 2, 2, 1200, 40, Vehicle.SAFE)
                .equals(new Car("T-1", 4, 2, 2, 1200, 40, Vehicle.SAFE)), "Vehicle identity by plate");
    }

    private static void testValidation() {
        mustFail(new Runnable() { public void run() {
            new Car("", 4, 2, 2, 1000, 20, Vehicle.SAFE);
        }}, "Empty plate is rejected");
        mustFail(new Runnable() { public void run() {
            new Bus("a", Double.NaN, 2, 2, 1000, 20, Vehicle.SAFE);
        }}, "NaN rejected");
        mustFail(new Runnable() { public void run() {
            new Car("a", 4, 2, 2, 1000, 20, "unknown");
        }}, "Invalid cargo rejected");
        mustFail(new Runnable() { public void run() {
            new Simulation(0);
        }}, "Zero checkpoints rejected");
        mustFail(new Runnable() { public void run() {
            VehicleFactory.create(7, "a", 4, 2, 2, 1000, 20, Vehicle.SAFE);
        }}, "Unknown vehicle subclass rejected");
    }

    private static void testSimulation() {
        Simulation simulator = new Simulation(1);
        Vehicle car = VehicleFactory.readyVehicles()[0];
        simulator.addVehicle(car);
        simulator.step();
        check(simulator.getCompleted() == 1, "Car completes after one minute");
        check(simulator.getAllowed() == 1 && simulator.getDenied() == 0,
                "Admission statistics remain correct");
        check(simulator.getEvents().size() == 3, "Arrival, start and finish events created");
        CheckPoint point = simulator.getPoints().get(0);
        check(point.getCount() == 1 && point.isFree(), "Checkpoint frees completed vehicle");
        simulator.setPointOpen(1, false);
        check(!point.isOpen(), "Checkpoint may be closed when free");
        mustFail(new Runnable() { public void run() {
            new Simulation(1).setPointOpen(2, false);
        }}, "Invalid checkpoint ID rejected");

        Simulation busy = new Simulation(1);
        busy.addVehicle(VehicleFactory.readyVehicles()[3]);
        busy.step();
        mustFail(new Runnable() { public void run() {
            busy.setPointOpen(1, false);
        }}, "Busy checkpoint cannot close");
        mustFail(new Runnable() { public void run() {
            busy.getPoints().get(0).setOpen(false);
        }}, "Busy checkpoint cannot be closed directly");
        Simulation ready = new Simulation(2);
        ready.runReady(5);
        check(ready.getCompleted() >= 2, "Old lab2 ready simulation preserved");
        Simulation generated = new Simulation(2);
        generated.runGenerated(15, 2, 3);
        check(!generated.getEvents().isEmpty(), "Old lab2 random stream preserved");
    }

    private static void testGenerator() {
        Lab3ObjectGenerator generator = new Lab3ObjectGenerator(42);
        check(generator.generateCar() instanceof Car, "Car generator");
        check(generator.generateTruck() instanceof Truck, "Truck generator");
        check(generator.generateBus() instanceof Bus, "Bus generator");
        check(generator.generateTrailer() instanceof Trailer, "Trailer generator");
        check(generator.generateCarWithTrailer() instanceof CarWithTrailer, "CarTrailer generator");
        Vehicle vehicle = generator.generateVehicle();
        check(vehicle.getCheckTime() > 0, "Vehicle generator");
        check(generator.generateCheckPoint(1).isOpen(), "Checkpoint generator");
        Event event = generator.generateEvent(vehicle, 5);
        check(event.getTime() == 5 && event.getVehicle() == vehicle, "Event generator");
        check(generator.generateCheckResult(vehicle).getVehicle() == vehicle,
                "CheckResult generator");
        check(generator.generateArrivalGenerator() instanceof Generator, "Arrival generator");
        Simulation root = generator.generateSimulation(7);
        check(root.getExperimentId() == 7, "Random root retains collection experiment ID");
        check(root.getPoints().size() > 0 && root.getQueueSize() > 0
                && !root.getEvents().isEmpty(), "Root simulation holds complete hierarchy");
    }

    private static void testCollections() {
        SimulationCollections collections = new SimulationCollections();
        collections.fill(10, new Lab3ObjectGenerator(5));
        check(collections.size() == 10, "ArrayList contains 10 simulations");
        check(collections.getHashMap().size() == 10, "HashMap contains 10 simulations");
        check(collections.containsSameObjects(), "Two collection types hold same root objects");
        check(collections.getArrayList().get(0) instanceof Simulation,
                "Collection element is Simulation, not Vehicle or String");
        check(collections.getHashMap().get(0) instanceof Simulation,
                "HashMap value has correct generic type");
        collections.fill(100, new Lab3ObjectGenerator(7));
        check(collections.size() == 100, "Second fill clears previous values");
    }

    private static void testSolvability() {
        SolvabilityService service = new SolvabilityService();
        List<Vehicle> queue = service.exampleQueue();
        check(service.simulate(queue, 1) == 16, "Initial four trailers need 16 minutes");
        check(service.simulate(queue, 2) == 8, "Two real points need 8 minutes");
        Vehicle stateful = new Car("S1", 4, 2, 2, 1000, 30, Vehicle.SAFE);
        stateful.setArriveTime(37);
        new CheckResult(stateful);
        String reasonBefore = stateful.getReason();
        check(service.simulate(Arrays.asList(stateful), 1) == 1,
                "Real simulation handles an independent copy");
        check(stateful.getArriveTime() == 37 && reasonBefore.equals(stateful.getReason()),
                "Solvability analysis does not mutate caller vehicles");
        SolvabilityService.Result solution = service.analyze(queue, 1, 2, 10);
        check(solution.isFeasible() && solution.getProposedPoints() == 2
                && solution.getProposedMinutes() == 8, "Open one real reserve point");
        SolvabilityService.Result impossible = service.analyze(queue, 1, 1, 10);
        check(!impossible.isFeasible(), "No fake unlimited checkpoints");
        SolvabilityService.Result already = service.analyze(queue, 2, 2, 10);
        check(already.isFeasible() && already.getProposedMinutes() == 8,
                "Already feasible cases do not require changes");
        check(!solution.getExplanation().isEmpty(), "A real proposal is explained");
        // Regression: shortest-first incorrectly reports no solution for 1,1,1,3.
        List<Vehicle> differentOrder = Arrays.<Vehicle>asList(
                new Car("A1", 4, 2, 2, 1500, 30, Vehicle.SAFE),
                new Car("A2", 4, 2, 2, 1500, 30, Vehicle.SAFE),
                new Car("A3", 4, 2, 2, 1500, 30, Vehicle.SAFE),
                new Bus("B1", 12, 2.5, 3.5, 12000, 30, Vehicle.SAFE));
        check(service.simulate(differentOrder, 2) == 4, "Original order takes 4 minutes");
        SolvabilityService.Result reorder = service.analyze(differentOrder, 2, 2, 3);
        check(reorder.isFeasible() && reorder.getProposedMinutes() == 3
                && reorder.getProposedPoints() == 2, "Longest-first finds real 3-minute schedule");
        check(service.analyze(differentOrder, 2, 2, 2).getExplanation()
                .contains("НЕ доказательство"), "No false impossibility claim");
    }

    private static void testInteractiveController() throws IOException {
        String commands = "1\n0\n";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ConsoleView view = new ConsoleView(new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(bytes, true, "UTF-8"));
        ErrMsgLog log = new ErrMsgLog(Paths.get("logs", "tests-interactive"));
        new Controller(view, log, Paths.get("results", "tests-interactive")).start();
        String output = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        check(output.contains("ПРОВЕРКА ГОТОВЫХ МАШИН"), "Original menu option 1 works");
        check(output.contains("Выход из программы"), "Exit option works");
        // The new menu must accept teacher-defined constraints without breaking MVC.
        String teacherInput = "5\n2\n4\n2\n2\n20\n0\n";
        ByteArrayOutputStream otherOutput = new ByteArrayOutputStream();
        ConsoleView otherView = new ConsoleView(new ByteArrayInputStream(
                teacherInput.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(otherOutput, true, "UTF-8"));
        new Controller(otherView, log, Paths.get("results", "tests-interactive")).start();
        String printed = new String(otherOutput.toByteArray(), StandardCharsets.UTF_8);
        check(printed.contains("РЕЖИМ РАЗРЕШИМОСТИ")
                && printed.contains("Выход из программы"), "Interactive solvability settings work");
    }

    private static void testBenchmarks() throws IOException {
        Path root = Paths.get("logs", "tests");
        ErrMsgLog messages = new ErrMsgLog(root);
        BenchmarkService runner = new BenchmarkService(messages, Paths.get("results", "tests"));
        runner.runArrayList(10);
        runner.runHashMap(10);
        verifyLog(root.resolve("ArrayList_10.log"), "add", "set", 10);
        verifyLog(root.resolve("HashMap_10.log"), "put", "replace", 10);
        int[] indices = BenchmarkService.randomUniqueIndices(1000, 100, 100);
        Set<Integer> selected = new HashSet<Integer>();
        for (int value : indices) {
            check(value >= 0 && value < 1000 && selected.add(value),
                    "Random indices are unique and in range");
        }
        check(messages.getErrorCount() == 0, "No false errors were logged");
        messages.error(new IllegalArgumentException("Тестовая ошибка"));
        check(messages.getErrorCount() == 1, "Central error counter works");
        check(Files.exists(root.resolve("errors.log")), "Errors are written to one handler");
    }

    private static void testCapacity() throws IOException {
        Path file = Paths.get("results", "tests", "capacity.txt");
        new ArrayListCapacityStudy().run(file);
        String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        check(content.contains("(oldCapacity * 3) / 2 + 1"), "Legacy capacity formula examined");
        check(content.contains("10;16;"), "Growth measured from old capacity 10 to legacy 16");
        check(content.contains("addWithGrowthNs"), "Actual expansion timed");
    }

    private static void testFullBenchmarks() throws IOException {
        Path logRoot = Paths.get("logs");
        List<BenchmarkService.Measurement> measurements =
                new BenchmarkService(new ErrMsgLog(logRoot), Paths.get("results")).runAll();
        check(measurements.size() == 10, "Five experiments per collection");
        for (int size : BenchmarkService.SIZES) {
            verifyLog(logRoot.resolve("ArrayList_" + size + ".log"), "add", "set", size);
            verifyLog(logRoot.resolve("HashMap_" + size + ".log"), "put", "replace", size);
        }
        Path summary = Paths.get("results", "summary.csv");
        check(Files.exists(summary), "CSV results generated");
        check(Files.readAllLines(summary, StandardCharsets.UTF_8).get(0)
                .contains("changeMedianNs"), "Summary CSV contains both operation medians");
        Path conclusion = Paths.get("results", "conclusion_ru.txt");
        check(Files.exists(conclusion), "Russian conclusion generated");
        String text = new String(Files.readAllBytes(conclusion), StandardCharsets.UTF_8);
        check(text.contains("Median") && text.contains("Total") && text.contains("Сопоставление"),
                "Conclusion compares both total times and medians across collections");
    }

    /** Checks that every operation and all totals really match the written log. */
    private static void verifyLog(Path file, String add, String change, int size) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        int additions = 0;
        int changes = 0;
        long addTotal = 0;
        long changeTotal = 0;
        Set<Integer> updated = new HashSet<Integer>();
        for (String line : lines) {
            if (line.startsWith(add + ", ID = ")) {
                String[] data = line.split(",");
                int key = Integer.parseInt(data[1].split("=")[1].trim());
                long nanoseconds = Long.parseLong(data[2].trim());
                check(key >= 0 && key < size && nanoseconds >= 0, "Valid add log entry");
                ++additions;
                addTotal += nanoseconds;
            } else if (line.startsWith(change + ", ID = ")) {
                String[] data = line.split(",");
                int key = Integer.parseInt(data[1].split("=")[1].trim());
                long nanoseconds = Long.parseLong(data[2].trim());
                check(key >= 0 && key < size && updated.add(key) && nanoseconds >= 0,
                        "Valid unique changed key");
                ++changes;
                changeTotal += nanoseconds;
            }
        }
        check(additions == size && changes == size / 10, "Correct addition/change counts");
        check(lines.contains(add + "TotalCount = " + size), "Add count summary");
        check(lines.contains(change + "TotalCount = " + size / 10), "Change count summary");
        check(lines.contains(add + "TotalTime = " + addTotal), "Add total matches measured rows");
        check(lines.contains(change + "TotalTime = " + changeTotal),
                "Change total matches measured rows");
        check(lines.contains("allOperationsTotalTime = " + (addTotal + changeTotal)),
                "Grand total matches log");
        check(lines.contains("allOperationsTotalCount = " + (size + size / 10)),
                "Grand operation count matches log");
        check(lines.stream().anyMatch(line -> line.startsWith(add + "MedianTime = ")),
                "Median included, separately from arithmetic average");
    }

    private static void check(boolean result, String description) {
        ++assertions;
        if (!result) throw new AssertionError("ПРОВАЛЕН ТЕСТ: " + description);
    }

    private static void mustFail(Runnable action, String description) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            check(true, description);
            return;
        } catch (IllegalStateException expected) {
            check(true, description);
            return;
        }
        check(false, description);
    }
}
