package controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import model.AppConfig;
import model.AppLog;
import model.CheckResult;
import model.Simulation;
import model.Vehicle;
import model.VehicleDatabase;
import model.VehicleFactory;
import tests.AutoTests;
import view.ConsoleView;

/** Управляет командами пользователя, не выполняя расчёты модели. */
public final class Controller {
    private final ConsoleView view;
    private final VehicleDatabase database;
    private final AppConfig config;
    private final AppLog log;

    public Controller(ConsoleView view, VehicleDatabase database, AppConfig config, AppLog log) {
        this.view = view;
        this.database = database;
        this.config = config;
        this.log = log;
    }

    /** Выполняет автоматические тесты до первого показа меню. */
    public void start() throws IOException {
        if (config.isAutotests()) {
            runTests();
        }
        while (true) {
            view.showMenu(config.isRoot());
            try {
                int choice = view.readInt("Выбор: ");
                log.debug("Команда меню: " + choice);
                if (choice == 0) {
                    view.showMessage("Выход из программы");
                    return;
                }
                handleChoice(choice);
            } catch (NoSuchElementException exception) {
                view.showMessage("Конец ввода");
                return;
            } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
                view.showError(exception);
                log.error(String.valueOf(exception.getMessage()));
            }
        }
    }

    /** Проверяет полномочия независимо от видимости пункта меню. */
    private void requireRoot() {
        if (!config.isRoot()) {
            throw new IllegalArgumentException("Доступ запрещён: необходима группа root");
        }
    }

    private void handleChoice(int choice) throws IOException {
        switch (choice) {
            case 1:
                for (Vehicle vehicle : VehicleFactory.readyVehicles()) {
                    view.showCheckResult(new CheckResult(vehicle));
                }
                break;
            case 2:
                view.showCheckResult(new CheckResult(readVehicle()));
                break;
            case 3:
                runSimulation();
                break;
            case 4:
                log.info("Чтение БД: объектов=" + database.load());
                view.showVehicles(database.getAll());
                break;
            case 5:
                database.save();
                log.info("Запись БД: объектов=" + database.getAll().size());
                view.showMessage("База данных сохранена");
                break;
            case 6:
                addVehicles();
                break;
            case 7:
                changeVehicle();
                break;
            case 8:
                deleteVehicle();
                break;
            case 9:
                view.showVehicles(database.getAll());
                break;
            case 10:
                view.showVehicle(database.get(view.readId("ID: ")));
                break;
            case 11:
                requireRoot();
                log.setDebug(!log.isDebug());
                log.info("Режим отладки: " + log.isDebug());
                view.showMessage("Отладка: " + log.isDebug());
                log.debug("Количество объектов: " + database.getAll().size());
                break;
            case 12:
                requireRoot();
                runTests();
                break;
            default:
                throw new IllegalArgumentException("Неверный выбор");
        }
    }

    private void addVehicles() throws IOException {
        int count = view.readInt("Количество новых объектов: ");
        if (count < 1) {
            throw new IllegalArgumentException("Количество должно быть положительным");
        }
        List<Vehicle> vehicles = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            view.showMessage("Объект " + (index + 1));
            vehicles.add(readVehicle());
        }
        List<Long> identifiers = database.putAll(vehicles);
        log.info("Запись БД: добавлены ID=" + identifiers);
        view.showMessage("Добавлены ID: " + identifiers);
    }

    private void changeVehicle() throws IOException {
        long id = view.readId("ID для изменения: ");
        view.showVehicle(database.get(id));
        database.change(id, readVehicle());
        log.info("Запись БД: изменён ID=" + id);
        view.showMessage("Объект изменён");
    }

    private void deleteVehicle() throws IOException {
        long id = view.readId("ID для удаления: ");
        database.del(id);
        log.info("Запись БД: удалён ID=" + id);
        view.showMessage("Объект удалён");
    }

    /** Передаёт введённые параметры фабрике; ограничения проверяет модель. */
    private Vehicle readVehicle() {
        view.showVehicleTypes();
        int type = view.readInt("Тип: ");
        if (type < 1 || type > 5) {
            throw new IllegalArgumentException("Неверный тип машины");
        }
        String number = view.readString("Номер: ");
        double length = view.readDouble("Длина (м): ");
        double width = view.readDouble("Ширина (м): ");
        double height = view.readDouble("Высота (м): ");
        double weight = view.readDouble("Масса (кг): ");
        int speed = view.readInt("Скорость (км/ч): ");
        String[] cargoTypes = Vehicle.cargoTypes();
        view.showCargoTypes(cargoTypes);
        int cargoIndex = view.readInt("Груз: ") - 1;
        if (cargoIndex < 0 || cargoIndex >= cargoTypes.length) {
            throw new IllegalArgumentException("Неверный груз");
        }
        return VehicleFactory.create(type, number, length, width, height, weight,
                speed, cargoTypes[cargoIndex]);
    }

    private void runSimulation() throws IOException {
        Simulation simulation = new Simulation(view.readInt("Количество пунктов: "));
        int duration = view.readInt("Время (мин): ");
        view.showSimulationMenu();
        switch (view.readInt("Режим: ")) {
            case 1:
                simulation.runReady(duration);
                break;
            case 2:
                int minInterval = view.readInt("Мин. интервал: ");
                int maxInterval = view.readInt("Макс. интервал: ");
                simulation.runGenerated(duration, minInterval, maxInterval);
                break;
            case 3:
                for (Vehicle vehicle : database.getAll().values()) {
                    simulation.addVehicle(vehicle);
                }
                simulation.run(duration);
                break;
            default:
                throw new IllegalArgumentException("Неверный режим");
        }
        view.showSimulationResult(simulation);
        log.debug("Моделирование: проверено=" + simulation.getCompleted()
                + ", время=" + simulation.getTime());
    }

    private void runTests() throws IOException {
        AutoTests.Report report = AutoTests.run();
        for (String line : report.getLines()) {
            view.showMessage(line);
            log.test(line);
        }
        if (!report.isSuccessful()) {
            log.error("Обнаружены ошибки автотестов");
        }
    }
}
