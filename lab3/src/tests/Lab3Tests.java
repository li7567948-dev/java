package tests;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Collections;

import controller.BenchmarkService;
import controller.ErrMsgLog;
import model.*;

/** No dependencies; executable offline via java tests.Lab3Tests. */
public final class Lab3Tests {
    private Lab3Tests() { }
    public static void main(String[] args) throws Exception {
        CheckpointGenerator generator = new CheckpointGenerator(123);
        CheckpointCollections c = new CheckpointCollections();
        c.fill(generator, 10);
        check(c.getArrayCheckpoints().size() == 10, "ArrayList size");
        check(c.getMapCheckpoints().size() == 10, "HashMap size");
        for (int i = 0; i < 10; i++) {
            check(c.getArrayCheckpoints().get(i) == c.getMapCheckpoints().get(i),
                    "Same main-class object");
        }
        Checkpoint cp = generator.generateCheckpoint(1, 50);
        check(cp.getVehicles().size() == 50, "Generator size");
        for (Vehicle v : cp.getVehicles()) {
            check(v.driverIsQualified(), "Correct driving license");
            check(v.getInspectionMinutes() > 0, "Nonzero processing time");
        }
        check(generator.generatePassengerCar().getType() == VehicleType.PASSENGER, "Passenger generator");
        check(generator.generateBus().getType() == VehicleType.BUS, "Bus generator");
        check(generator.generateTruck().getType() == VehicleType.TRUCK, "Truck generator");
        check(generator.generateInspectionLane(1, true).isActive(), "Lane generator");
        check(generator.generateDriver(VehicleType.BUS).canDrive(VehicleType.BUS), "Driver generator");
        Checkpoint example = generator.generateSolvabilityExample();
        SolvabilityService.Result result = new SolvabilityService().analyze(example);
        check(!result.isFeasible(), "Unsatisfied initial deadline");
        check(result.getCurrentMinutes() == 32, "Expected 32 minutes in active lanes");
        check(result.getExplanation().contains("резервную"), "Valid reserve-lane solution");

        Driver passengerOnly = new Driver("Тест", 5, EnumSet.of(VehicleType.PASSENGER));
        Truck illegal = new Truck("ТТ-1", passengerOnly, false);
        Checkpoint invalid = new Checkpoint("Неверные права", 30,
                Arrays.<Vehicle>asList(illegal),
                Arrays.asList(generator.generateInspectionLane(1, true)));
        check(new SolvabilityService().analyze(invalid).getExplanation().contains("не имеет"),
                "Driver eligibility enforced");
        Checkpoint impossible = new Checkpoint("Мало полос", 5,
                example.getVehicles(), Arrays.asList(generator.generateInspectionLane(1, true)));
        check(!new SolvabilityService().analyze(impossible).isFeasible(),
                "No physically impossible remedy");

        Path logDir = Paths.get("logs", "test-run");
        try (ErrMsgLog logger = new ErrMsgLog(logDir)) {
            logger.openOperationLog("TestArray", 10);
            logger.operation("add", 0, 30);
            logger.operation("add", 1, 10);
            logger.summary("add", new long[] {30, 10});
            logger.finishOperationLog();
            String content = new String(Files.readAllBytes(logDir.resolve("TestArray_10.log")), "UTF-8");
            check(content.contains("addMedianTime = 20"), "Median calculation");
            check(content.contains("addAverageTime = 20.0"), "Average calculation");
            check(content.contains("allOperationsTotalCount = 2"), "Overall count");
            check(content.contains("allOperationsTotalTime = 40"), "Overall total time");
            logger.error(new IllegalArgumentException("Ошибка теста"));
            check(logger.getErrorCount() == 1, "Error counter");
        }
        if (args.length > 0 && "--full".equals(args[0])) {
            try (ErrMsgLog logger = new ErrMsgLog(Paths.get("logs"))) {
                new BenchmarkService(logger).runAll();
            }
            for (String kind : Arrays.asList("ArrayList", "HashMap")) {
                for (int n : BenchmarkService.COUNTS) {
                    String content = new String(Files.readAllBytes(
                            Paths.get("logs", kind + "_" + n + ".log")), "UTF-8");
                    String add = kind.equals("ArrayList") ? "add" : "put";
                    String change = kind.equals("ArrayList") ? "set" : "replace";
                    check(content.contains(add + "TotalCount = " + n), "Log add count");
                    check(content.contains(change + "TotalCount = " + n / 10), "Log 10% count");
                    verifyLog(content, add, change, n);
                }
            }
        }
        System.out.println("ВСЕ ТЕСТЫ ПРОЙДЕНЫ.");
    }
    private static void verifyLog(String content, String add, String change, int size) {
        long addTime = 0;
        long changeTime = 0;
        int added = 0;
        int changed = 0;
        java.util.Set<Integer> updatedKeys = new java.util.HashSet<Integer>();
        for (String line : content.split("\\R")) {
            if (line.startsWith(add + ", ID = ")) {
                String[] fields = line.split(",");
                int key = Integer.parseInt(fields[1].split("=")[1].trim());
                long elapsed = Long.parseLong(fields[2].trim());
                check(key >= 0 && key < size && elapsed >= 0, "Addition measurement");
                addTime += elapsed;
                added++;
            } else if (line.startsWith(change + ", ID = ")) {
                String[] fields = line.split(",");
                int key = Integer.parseInt(fields[1].split("=")[1].trim());
                long elapsed = Long.parseLong(fields[2].trim());
                check(key >= 0 && key < size && elapsed >= 0, "Change measurement");
                check(updatedKeys.add(key), "Changes use unique random keys");
                changeTime += elapsed;
                changed++;
            }
        }
        check(added == size && changed == size / 10, "Exact operation count");
        check(content.contains(add + "TotalTime = " + addTime), "Addition total matches records");
        check(content.contains(change + "TotalTime = " + changeTime), "Change total matches records");
        check(content.contains("allOperationsTotalTime = " + (addTime + changeTime)), "All operations total");
        check(content.contains("allOperationsTotalCount = " + (size + size / 10)), "All operations count");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError("TEST FAILED: " + message);
    }
}
