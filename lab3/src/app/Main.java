package app;

import controller.CheckpointController;
import controller.ErrMsgLog;
import view.CheckpointView;
import java.nio.file.Paths;

/** Run --demo, --benchmark, or --all (default). */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        boolean failed = false;
        try (ErrMsgLog logger = new ErrMsgLog(Paths.get("logs"))) {
            try {
                String mode = args.length == 0 ? "--all" : args[0];
                if (!("--all".equals(mode) || "--demo".equals(mode)
                        || "--solvability".equals(mode) || "--benchmark".equals(mode)) || args.length > 1) {
                    throw new IllegalArgumentException(
                            "Использование: --demo | --solvability | --benchmark | --all");
                }
                new CheckpointController(new CheckpointView(), logger).run(mode.substring(2));
            } catch (Exception exception) {
                // All application errors are handled by the central message class.
                logger.error(exception);
            }
            failed = logger.getErrorCount() != 0;
        } catch (Exception exception) {
            ErrMsgLog.reportStartupError(exception);
            failed = true;
        }
        if (failed) System.exit(1);
    }
}
