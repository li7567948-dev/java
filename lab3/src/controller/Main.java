package controller;

import java.io.IOException;
import java.nio.file.Paths;

import view.ConsoleView;

/**
 * Run in Eclipse as: Run As -> Java Application.
 * Without arguments, starts the interactive menu from laboratory work No. 2.
 * Optional arguments: --benchmark, --solvability, --capacity, --demo.
 */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        ConsoleView view = new ConsoleView();
        ErrMsgLog messages;
        try {
            messages = new ErrMsgLog(Paths.get("logs"));
        } catch (IOException exception) {
            System.err.println(ErrMsgLog.initializationError(exception));
            return;
        }
        Controller controller = new Controller(view, messages, Paths.get("results"));
        try {
            if (args.length == 0) {
                controller.start();
            } else if (args.length == 1) {
                switch (args[0]) {
                    case "--benchmark": controller.runBenchmarks(); break;
                    case "--solvability": controller.runSolvability(); break;
                    case "--capacity": controller.runCapacityStudy(); break;
                    case "--demo": controller.demonstrateCollections(); break;
                    default: throw new IllegalArgumentException("Неизвестный аргумент: " + args[0]);
                }
            } else {
                throw new IllegalArgumentException("Используйте не более одного аргумента");
            }
        } catch (Exception exception) {
            view.showMessage(messages.error(exception));
        }
    }
}
