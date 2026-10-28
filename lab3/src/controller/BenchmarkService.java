package controller;

import model.Checkpoint;
import model.CheckpointGenerator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/** Runs the exact sizes and 10% random unique replacements from the handout. */
public final class BenchmarkService {
    public static final int[] COUNTS = {10, 100, 1000, 10000, 100000};
    private final ErrMsgLog logger;

    public BenchmarkService(ErrMsgLog logger) { this.logger = logger; }

    public void runAll() throws IOException {
        CheckpointGenerator generator = new CheckpointGenerator(20261027L);
        for (int count : COUNTS) runArrayList(generator, count);
        for (int count : COUNTS) runHashMap(generator, count);
        new ArrayListCapacityStudy(logger).run();
    }

    private void runArrayList(CheckpointGenerator generator, int count) throws IOException {
        ArrayList<Checkpoint> collection = new ArrayList<Checkpoint>();
        logger.openOperationLog("ArrayList", count);
        try {
            long[] addTimes = new long[count];
            for (int i = 0; i < count; i++) {
                Checkpoint checkpoint = generator.generateCheckpoint(i + 1, 1);
                long t0 = System.nanoTime();
                collection.add(checkpoint);
                addTimes[i] = System.nanoTime() - t0;
                logger.operation("add", i, addTimes[i]);
            }
            logger.summary("add", addTimes);
            int[] keys = selectRandomKeys(count, 839L + count);
            long[] setTimes = new long[keys.length];
            for (int i = 0; i < keys.length; i++) {
                Checkpoint newObject = generator.generateCheckpoint(i + 1, 1);
                long t0 = System.nanoTime();
                collection.set(keys[i], newObject);
                setTimes[i] = System.nanoTime() - t0;
                logger.operation("set", keys[i], setTimes[i]);
            }
            logger.summary("set", setTimes);
        } finally {
            logger.finishOperationLog();
        }
        logger.info("ArrayList: " + count + " объектов — лог создан.");
    }

    private void runHashMap(CheckpointGenerator generator, int count) throws IOException {
        HashMap<Integer, Checkpoint> collection = new HashMap<Integer, Checkpoint>();
        logger.openOperationLog("HashMap", count);
        try {
            long[] addTimes = new long[count];
            for (int i = 0; i < count; i++) {
                Checkpoint checkpoint = generator.generateCheckpoint(i + 1, 1);
                long t0 = System.nanoTime();
                collection.put(i, checkpoint);
                addTimes[i] = System.nanoTime() - t0;
                logger.operation("put", i, addTimes[i]);
            }
            logger.summary("put", addTimes);
            int[] keys = selectRandomKeys(count, 839L + count);
            long[] replaceTimes = new long[keys.length];
            for (int i = 0; i < keys.length; i++) {
                Checkpoint checkpoint = generator.generateCheckpoint(i + 1, 1);
                long t0 = System.nanoTime();
                Checkpoint previous = collection.replace(keys[i], checkpoint);
                replaceTimes[i] = System.nanoTime() - t0;
                if (previous == null) throw new IllegalStateException("Ключ не найден: " + keys[i]);
                logger.operation("replace", keys[i], replaceTimes[i]);
            }
            logger.summary("replace", replaceTimes);
        } finally {
            logger.finishOperationLog();
        }
        logger.info("HashMap: " + count + " объектов — лог создан.");
    }

    private static int[] selectRandomKeys(int count, long seed) {
        List<Integer> indexes = new ArrayList<Integer>(count);
        for (int i = 0; i < count; i++) indexes.add(i);
        Collections.shuffle(indexes, new Random(seed));
        int[] keys = new int[count / 10];
        for (int i = 0; i < keys.length; i++) keys[i] = indexes.get(i);
        return keys;
    }
}
