package tests;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import model.AppConfig;
import model.AppLog;
import model.Generator;
import model.Simulation;
import model.Truck;
import model.Vehicle;
import model.VehicleDatabase;
import model.VehicleFactory;

/** Автотесты с контрольными данными; рабочая БД не затрагивается. */
public final class AutoTests {

    private AutoTests() {
    }
    @FunctionalInterface
    private interface Test {
        void run() throws Exception;
    }

    public static final class Report {
        private final List<String> lines = new ArrayList<>();
        private int passed, failed;
        private void check(String name, Test test) {
            try {
                test.run();
                passed++;
                lines.add("PASS: " + name);
            } catch (Exception | AssertionError exception) {
                failed++;
                lines.add("FAIL: " + name + " — " + exception.getMessage());
            }
        }
        public List<String> getLines() {
            return Collections.unmodifiableList(lines);
        }
        public boolean isSuccessful() {
            return failed == 0;
        }
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void rejects(Class<? extends Exception> type, Test action) throws Exception {
        try {
            action.run();
        } catch (Exception exception) {
            if (type.isInstance(exception)) {
                return;
            }
            throw exception;
        }
        throw new AssertionError("Ожидалось исключение " + type.getSimpleName());
    }

    private static Vehicle vehicle(int type, String cargo, int speed) {
        return VehicleFactory.create(type, "А 123 БВ", 4, 1.8, 1.5, 1500, speed, cargo);
    }

    public static Report run() {
        Report report = new Report();
        report.check("Границы размеров и скорости пяти классов", () -> {
            double[] lengths = {
                6, 12, 15, 20, 10
            }, widths = {
                2.5, 2.5, 2.55, 2.55, 2.2
            }, heights = {
                3, 4, 4, 4, 3
            }, weights = {
                3500, 40000, 18000, 44000, 5000
            };
            int[] speeds = {
                90, 70, 60, 30, 40
            };
            for (int index = 0; index < 5; index++) {
                Vehicle vehicle = VehicleFactory.create(index + 1, "A", lengths[index], widths[index],
                        heights[index], weights[index], speeds[index], Vehicle.SAFE);
                expect(vehicle.pass(), "Граница типа " + (index + 1));
                expect(!VehicleFactory.create(index + 1, "B", lengths[index] + 0.1, widths[index],
                        heights[index], weights[index], speeds[index], Vehicle.SAFE).pass(), "Превышение длины");
                expect(!VehicleFactory.create(index + 1, "B", lengths[index], widths[index] + 0.1,
                        heights[index], weights[index], speeds[index], Vehicle.SAFE).pass(), "Превышение ширины");
                expect(!VehicleFactory.create(index + 1, "B", lengths[index], widths[index], heights[index]
                        + 0.1, weights[index], speeds[index], Vehicle.SAFE).pass(), "Превышение высоты");
                expect(!VehicleFactory.create(index + 1, "B", lengths[index], widths[index], heights[index],
                        weights[index] + 1, speeds[index], Vehicle.SAFE).pass(), "Превышение массы");
                expect(!VehicleFactory.create(index + 1, "B", lengths[index], widths[index], heights[index],
                        weights[index], speeds[index] + 1, Vehicle.SAFE).pass(), "Превышение скорости");
            }
        });
        report.check("Ограничения опасного груза", () -> {
            expect(vehicle(2, Vehicle.FIRE, 20).pass(), "20 км/ч");
            expect(!vehicle(2, Vehicle.FIRE, 21).pass(), "21 км/ч");
            expect(!vehicle(3, Vehicle.FIRE, 10).pass(), "Автобус");
            expect(vehicle(4, Vehicle.BOOM, 10).pass(), "Трейлер 10");
            expect(!vehicle(4, Vehicle.BOOM, 11).pass(), "Трейлер 11");
            expect(!vehicle(5, Vehicle.CHEMICAL, 10).pass(), "Прицеп");
        });
        report.check("Время проверки: 1, 2, 3, 4 минуты и надбавки", () -> {
            double[] lengths = {
                5, 10, 15, 16
            };
            for (int index = 0; index < 4; index++) {
                expect(VehicleFactory.create(4, "A", lengths[index], 2, 2, 1500, 10, Vehicle.SAFE).getCheckTime() == index
                        + 1, "Длина");
            }
            String[] cargo = Vehicle.cargoTypes();
            int[] expected = {
                1, 3, 4, 6, 3
            };
            for (int index = 0; index < cargo.length; index++) {
                expect(vehicle(2, cargo[index], 10).getCheckTime() == expected[index], "Груз");
            }
        });
        report.check("Повторная проверка без накопления причин", () -> {
            Vehicle vehicle = vehicle(3, Vehicle.FIRE, 21);
            expect(!vehicle.pass(), "Запрет");
            String first = vehicle.getReason();
            vehicle.pass();
            expect(first.equals(vehicle.getReason()), "Повтор причины");
        });
        report.check("Некорректные значения", () -> {
            rejects(IllegalArgumentException.class, () -> VehicleFactory.create(1, "A", Double.NaN,
                    2, 2, 1000, 10, Vehicle.SAFE));
            rejects(IllegalArgumentException.class, () -> VehicleFactory.create(1, "A\tB", 4, 2, 2,
                    1000, 10, Vehicle.SAFE));
            rejects(IllegalArgumentException.class, () -> vehicle(1, Vehicle.SAFE, -1));
            rejects(IllegalArgumentException.class, () -> vehicle(6, Vehicle.SAFE, 10));
        });
        report.check("Очередь: 3 машины, 2 разрешения, 1 отказ", () -> {
            Simulation simulation = new Simulation(1);
            simulation.runReady(20);
            expect(simulation.getCompleted() == 3 && simulation.getAllowed() == 2 && simulation.getDenied() == 1
                    && simulation.getActiveCount() == 0 && simulation.getQueueSize() == 0, "Итоги очереди");
        });
        report.check("Занятый пункт нельзя закрыть; исходный объект не меняется", () -> {
            Vehicle vehicle = vehicle(2, Vehicle.BOOM, 10);
            vehicle.setArriveTime(99);
            Simulation simulation = new Simulation(1);
            simulation.addVehicle(vehicle);
            simulation.step();
            rejects(IllegalStateException.class, () -> simulation.setPointOpen(1, false));
            expect(vehicle.getArriveTime() == 99 && vehicle.getReason().isEmpty(), "Исходный объект изменён");
        });
        report.check("Генератор: фиксированный интервал 2 минуты", () -> {
            Generator generator = new Generator(2, 2, new Random(1));
            expect(generator.newCars(0).size() == 1 && generator.newCars(1).isEmpty() && generator.newCars(2).size() == 1,
                    "Интервалы");
        });
        report.check("БД: пять классов, UTF-8, все поля, ID, повторный запуск", () -> {
            Path dir = Files.createTempDirectory("lab4-db-");
            Path file = dir.resolve("db.txt");
            try {
                VehicleDatabase database = new VehicleDatabase(file);
                List<Vehicle> data = new ArrayList<>(Arrays.asList(VehicleFactory.readyVehicles()));
                Vehicle russian = vehicle(1, Vehicle.SAFE, 10);
                russian.setArriveTime(7);
                data.add(russian);
                List<Long> ids = database.putAll(data);
                expect(ids.size() == 6, "Количество");
                VehicleDatabase reload = new VehicleDatabase(file);
                expect(reload.load() == 6, "Чтение");
                for (int index = 0; index < data.size(); index++) {
                    Vehicle expectedVehicle = data.get(index), actualVehicle = reload.get(ids.get(index));
                    expect(expectedVehicle.getClass() == actualVehicle.getClass() && expectedVehicle.toString().equals(actualVehicle.toString())
                            && expectedVehicle.getArriveTime() == actualVehicle.getArriveTime(), "Данные объекта "
                            + index);
                }
                reload.change(1, vehicle(2, Vehicle.SAFE, 15));
                reload.del(6);
                VehicleDatabase again = new VehicleDatabase(file);
                again.load();
                expect(again.get(1) instanceof Truck && again.getAll().size() == 5, "Изменение и удаление");
                expect(again.put(russian) == 7, "ID не используется повторно");
                rejects(IllegalArgumentException.class, () -> again.del(999));
                String valid = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                Files.write(file, (valid + "bad row\n").getBytes(StandardCharsets.UTF_8));
                rejects(IOException.class, again::load);
                expect(again.getAll().size() == 6, "Потеря данных при ошибке чтения");
            } finally {
                Files.deleteIfExists(file);
                Files.deleteIfExists(dir);
            }
        });
        report.check("Отказ записи сохраняет состояние БД", () -> {
            Path dir = Files.createTempDirectory("lab4-failure-");
            Path bad = dir.resolve("not-directory");
            try {
                Files.createFile(bad);
                VehicleDatabase database = new VehicleDatabase(bad.resolve("db.txt"));
                rejects(IOException.class, () -> database.put(vehicle(1, Vehicle.SAFE, 10)));
                expect(database.getAll().isEmpty(), "Состояние изменилось");
            } finally {
                Files.deleteIfExists(bad);
                Files.deleteIfExists(dir);
            }
        });
        report.check("Настройки root/user, пароль и строгие флаги", () -> {
            Path file = Files.createTempFile("lab4-config-", ".properties");
            try {
                String base = "login=тест\npassword=demo\ndebug=true\nautotests=true\ngroup=";
                Files.write(file, (base + "root\n").getBytes(StandardCharsets.UTF_8));
                AppConfig configuration = AppConfig.load(file);
                expect(configuration.isRoot() && configuration.isDebug() && configuration.isAutotests()
                        && configuration.authenticate("тест", "demo") && !configuration.authenticate("тест",
                        "bad"), "root");
                Files.write(file, (base + "user\n").getBytes(StandardCharsets.UTF_8));
                expect(!AppConfig.load(file).isRoot(), "user");
                Files.write(file, (base + "unknown\n").getBytes(StandardCharsets.UTF_8));
                rejects(IllegalArgumentException.class, () -> AppConfig.load(file));
            } finally {
                Files.deleteIfExists(file);
            }
        });
        report.check("Лог: добавление, метки времени, отключение DEBUG", () -> {
            Path file = Files.createTempFile("lab4-log-", ".txt");
            try {
                try (AppLog log = new AppLog(file)) {
                    log.info("FIRST");
                    log.debug("HIDDEN");
                }
                try (AppLog log = new AppLog(file)) {
                    log.setDebug(true);
                    log.debug("SECOND");
                    log.test("TEST");
                }
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                expect(lines.size() == 3 && lines.get(0).contains("FIRST") && lines.get(1).contains("SECOND")
                        && lines.get(2).contains("TEST"), "Добавление");
                expect(lines.get(0).matches("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3} .*"),
                        "Дата");
            } finally {
                Files.deleteIfExists(file);
            }
        });
        report.check("Пустая БД и удаление всех объектов: ID сохраняется", () -> {
            Path file = Files.createTempFile("lab4-empty-", ".txt");
            try {
                VehicleDatabase database = new VehicleDatabase(file);
                database.save();
                expect(database.load() == 0, "Пустая БД");
                long id = database.put(vehicle(1, Vehicle.SAFE, 10));
                database.del(id);
                VehicleDatabase reopened = new VehicleDatabase(file);
                expect(reopened.load() == 0, "Удалены все объекты");
                expect(reopened.put(vehicle(1, Vehicle.SAFE, 10)) == id + 1,
                        "ID не должен использоваться повторно");
            } finally {
                Files.deleteIfExists(file);
            }
        });
        report.check("Повторный ID и повреждённая кодировка не меняют модель", () -> {
            Path file = Files.createTempFile("lab4-duplicate-", ".txt");
            try {
                VehicleDatabase database = new VehicleDatabase(file);
                database.put(vehicle(1, Vehicle.SAFE, 10));
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                lines.add(lines.get(1));
                Files.write(file, lines, StandardCharsets.UTF_8);
                rejects(IOException.class, database::load);
                expect(database.getAll().size() == 1, "Повторный ID");
                Files.write(file, new byte[] {(byte) 0xFF});
                rejects(IOException.class, database::load);
                expect(database.getAll().size() == 1, "Ошибка UTF-8");
            } finally {
                Files.deleteIfExists(file);
            }
        });
        report.check("Отсутствующий файл и все восемь комбинаций настроек", () -> {
            Path file = Files.createTempFile("lab4-flags-", ".properties");
            try {
                Files.delete(file);
                rejects(IOException.class, () -> AppConfig.load(file));
                for (String role : Arrays.asList("root", "user")) {
                    for (boolean debug : new boolean[] {false, true}) {
                        for (boolean autotests : new boolean[] {false, true}) {
                            String content = "login=demo\npassword=lab4\ngroup=" + role
                                    + "\ndebug=" + debug + "\nautotests=" + autotests + "\n";
                            Files.write(file, content.getBytes(StandardCharsets.UTF_8));
                            AppConfig config = AppConfig.load(file);
                            expect(config.isRoot() == role.equals("root")
                                    && config.isDebug() == debug && config.isAutotests() == autotests,
                                    "Комбинация настроек");
                            expect(!config.authenticate("wrong", "lab4")
                                    && !config.authenticate("demo", "wrong"), "Неверный вход");
                        }
                    }
                }
                Files.write(file, "login=demo\n".getBytes(StandardCharsets.UTF_8));
                rejects(IllegalArgumentException.class, () -> AppConfig.load(file));
                Files.write(file, ("login=demo\npassword=lab4\ngroup=user\n"
                        + "debug=maybe\nautotests=false\n").getBytes(StandardCharsets.UTF_8));
                rejects(IllegalArgumentException.class, () -> AppConfig.load(file));
            } finally {
                Files.deleteIfExists(file);
            }
        });
        report.lines.add("ИТОГО: " + report.passed + " успешно, " + report.failed + " ошибок");
        return report;
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        Report report = run();
        PrintStream output = new PrintStream(System.out, true, "UTF-8");
        report.getLines().forEach(output::println);
        if (!report.isSuccessful()) {
            System.exit(1);
        }
    }
}
