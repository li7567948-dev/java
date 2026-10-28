package controller;

import model.Checkpoint;
import model.CheckpointCollections;
import model.CheckpointGenerator;
import model.SolvabilityService;
import view.CheckpointView;

/** MVC controller: coordinates model, view and logger without mixing responsibilities. */
public final class CheckpointController {
    private final CheckpointView view;
    private final ErrMsgLog logger;
    private final CheckpointGenerator generator = new CheckpointGenerator(314159L);

    public CheckpointController(CheckpointView view, ErrMsgLog logger) {
        this.view = view;
        this.logger = logger;
    }

    public void run(String mode) {
        try {
            view.header();
            if ("all".equals(mode) || "demo".equals(mode)) runDemo();
            if ("solvability".equals(mode)) runSolvability();
            if ("all".equals(mode) || "benchmark".equals(mode)) {
                new BenchmarkService(logger).runAll();
            }
            logger.info("Количество ошибок: " + logger.getErrorCount());
        } catch (Exception exception) {
            logger.error(exception);
        }
    }

    private void runDemo() {
        CheckpointCollections collections = new CheckpointCollections();
        collections.fill(generator, 3);
        view.collections(collections.getArrayCheckpoints().size(),
                collections.getMapCheckpoints().size());
        // One and the same object of the MAIN class is held by both collections.
        if (collections.getArrayCheckpoints().get(0) !=
                collections.getMapCheckpoints().get(0)) {
            throw new IllegalStateException("Нарушена идентичность объектов коллекций");
        }
        Checkpoint generated = collections.getArrayCheckpoints().get(0);
        view.checkpoint(generated);
        view.solvability(new SolvabilityService().analyze(generated));
        runSolvability();
    }

    /** Independent operating mode required by task 7.2. */
    private void runSolvability() {
        Checkpoint solvabilityExample = generator.generateSolvabilityExample();
        view.checkpoint(solvabilityExample);
        view.solvability(new SolvabilityService().analyze(solvabilityExample));
    }
}
