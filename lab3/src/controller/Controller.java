package controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

import model.CheckResult;
import model.Lab3ObjectGenerator;
import model.Simulation;
import model.SimulationCollections;
import model.SolvabilityService;
import model.Vehicle;
import model.VehicleFactory;
import view.ConsoleView;

/**
 * MVC controller. Keeps all three original lab No. 2 menu options;
 * adds benchmarking, capacity study and an explicit solvability mode.
 */
public final class Controller {
    private final ConsoleView view;
    private final ErrMsgLog messages;
    private final Path resultsDirectory;
    private final Vehicle[] readyVehicles;

    public Controller(ConsoleView view, ErrMsgLog messages, Path resultsDirectory) {
        if (view == null || messages == null || resultsDirectory == null) {
            throw new IllegalArgumentException("Не заданы зависимости контроллера");
        }
        this.view = view;
        this.messages = messages;
        this.resultsDirectory = resultsDirectory;
        this.readyVehicles = VehicleFactory.readyVehicles();
    }

    public void start() {
        while (true) {
            try {
                view.showMenu();
                int choice = readInt("Выбор: ");
                if (choice == 0) {
                    view.showMessage("Выход из программы");
                    return;
                }
                handleChoice(choice);
            } catch (NoSuchElementException endOfInput) {
                view.showMessage("Конец ввода. Завершение программы.");
                return;
            } catch (Exception exception) {
                view.showMessage(messages.error(exception));
            }
        }
    }

    private void handleChoice(int choice) throws IOException {
        switch (choice) {
            case 1: checkReadyVehicles(); break;
            case 2: checkOwnVehicle(); break;
            case 3: runSimulation(); break;
            case 4: runBenchmarks(); break;
            case 5: runSolvability(); break;
            case 6: runCapacityStudy(); break;
            default: throw new IllegalArgumentException("Неверный пункт меню: " + choice);
        }
    }

    private void checkReadyVehicles() {
        view.showMessage("===== ПРОВЕРКА ГОТОВЫХ МАШИН =====");
        for (Vehicle vehicle : readyVehicles) {
            view.showCheckResult(new CheckResult(vehicle));
            view.showMessage("-------------------------------------");
        }
    }

    private void checkOwnVehicle() {
        view.showMessage("===== ПРОВЕРКА СВОЕЙ МАШИНЫ =====");
        view.showMessage("1 — Легковой автомобиль");
        view.showMessage("2 — Грузовой автомобиль");
        view.showMessage("3 — Автобус");
        view.showMessage("4 — Трейлер");
        view.showMessage("5 — Автомобиль с прицепом");
        int type = readInt("Тип машины: ");
        if (type < 1 || type > 5) throw new IllegalArgumentException("Неизвестный тип машины");
        String number = view.readLine("Номер: ");
        double length = readDouble("Длина (м): ");
        double width = readDouble("Ширина (м): ");
        double height = readDouble("Высота (м): ");
        double weight = readDouble("Масса (кг): ");
        int speed = readInt("Скорость (км/ч): ");
        for (int index = 0; index < Vehicle.CARGO.length; ++index) {
            view.showMessage((index + 1) + " — " + Vehicle.CARGO[index]);
        }
        int cargo = readInt("Тип груза: ");
        if (cargo < 1 || cargo > Vehicle.CARGO.length) {
            throw new IllegalArgumentException("Неизвестный тип груза");
        }
        Vehicle vehicle = VehicleFactory.create(type, number, length, width, height,
                weight, speed, Vehicle.CARGO[cargo - 1]);
        view.showCheckResult(new CheckResult(vehicle));
    }

    private void runSimulation() {
        int pointCount = readInt("Количество пунктов: ");
        int duration = readInt("Время моделирования (мин): ");
        if (pointCount < 1 || pointCount > 100 || duration < 1 || duration > 100000) {
            throw new IllegalArgumentException("Неверные параметры моделирования");
        }
        Simulation simulation = new Simulation(pointCount);
        view.showMessage("1 — Три готовые машины");
        view.showMessage("2 — Случайный поток машин");
        int mode = readInt("Режим: ");
        if (mode == 1) {
            simulation.runReady(duration);
        } else if (mode == 2) {
            int minimum = readInt("Минимальный интервал (мин): ");
            int maximum = readInt("Максимальный интервал (мин): ");
            simulation.runGenerated(duration, minimum, maximum);
        } else {
            throw new IllegalArgumentException("Неизвестный режим моделирования");
        }
        view.showSimulation(simulation);
    }

    public void demonstrateCollections() {
        SimulationCollections collections = new SimulationCollections();
        collections.fill(10, new Lab3ObjectGenerator(10));
        view.showMessage("Коллекции основного класса Simulation:");
        view.showMessage("ArrayList<Simulation>: " + collections.getArrayList().size());
        view.showMessage("HashMap<Integer, Simulation>: " + collections.getHashMap().size());
        view.showMessage("Объекты совпадают: " + collections.containsSameObjects());
    }

    public void runBenchmarks() throws IOException {
        demonstrateCollections();
        view.showMessage("Начаты эксперименты 10, 100, 1000, 10000, 100000...");
        List<BenchmarkService.Measurement> measurements =
                new BenchmarkService(messages, resultsDirectory).runAll();
        for (BenchmarkService.Measurement value : measurements) {
            view.showMessage(value.getKind() + " N=" + value.getSize()
                    + " add/putTotalNs=" + value.getAddTotal()
                    + " set/replaceTotalNs=" + value.getChangeTotal());
        }
        view.showMessage("Созданы 10 логов и results/summary.csv; errors=" + messages.getErrorCount());
    }

    public void runSolvability() {
        SolvabilityService service = new SolvabilityService();
        view.showMessage("Четыре трейлера по 4 мин; 1 рабочий и 1 резервный пункт.");
        view.showSolvability(service.analyze(service.exampleQueue(), 1, 2, 10));
    }

    public void runCapacityStudy() throws IOException {
        Path report = resultsDirectory.resolve("arraylist_capacity.txt");
        new ArrayListCapacityStudy().run(report);
        view.showMessage("Файл исследования: " + report);
    }

    private int readInt(String prompt) {
        String value = view.readLine(prompt);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Ожидалось целое число: '" + value + "'", exception);
        }
    }

    private double readDouble(String prompt) {
        String value = view.readLine(prompt).replace(',', '.');
        try {
            double number = Double.parseDouble(value);
            if (!Double.isFinite(number)) throw new NumberFormatException(value);
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Ожидалось конечное число: '" + value + "'", exception);
        }
    }
}
