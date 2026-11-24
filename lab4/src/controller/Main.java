package controller;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.NoSuchElementException;

import model.AppConfig;
import model.AppLog;
import model.VehicleDatabase;
import model.VehicleFactory;
import view.ConsoleView;

/** Точка входа: загрузка настроек, аутентификация и запуск MVC. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try (AppLog log = new AppLog(Paths.get("logs", "application.log"));
                ConsoleView view = new ConsoleView(System.in, new PrintStream(System.out, true, "UTF-8"))) {
            try {
                if (args.length > 1) {
                    throw new IllegalArgumentException("Укажите не более одного файла настроек");
                }
                log.info("Запуск приложения");
                Path configPath = Paths.get(args.length == 0 ? "config.properties" : args[0]);
                AppConfig config = AppConfig.load(configPath);
                log.setDebug(config.isDebug());
                log.info("Старт программы, логин из настроек: " + config.getLogin());
                log.debug("Группа=" + config.getRole() + ", автотесты=" + config.isAutotests());
                String login = view.readString("Логин: ");
                String password = view.readString("Пароль: ");
                if (!config.authenticate(login, password)) {
                    log.error("Неудачная аутентификация: " + login);
                    view.showMessage("Неверный логин или пароль");
                    return;
                }
                log.info("Вход выполнен: " + login);
                view.showMessage("Добро пожаловать — " + config.getLogin());
                VehicleDatabase database = openDatabase(log);
                new Controller(view, database, config, log).start();
            } catch (IOException | IllegalArgumentException | IllegalStateException | NoSuchElementException exception) {
                log.error(String.valueOf(exception.getMessage()));
                view.showError(exception);
            } finally {
                log.info("Завершение программы");
            }
        } catch (IOException exception) {
            System.err.println("Ошибка журнала: " + exception.getMessage());
        }
    }

    /** При повреждении существующего файла запуск прекращается без его перезаписи. */
    private static VehicleDatabase openDatabase(AppLog log) throws IOException {
        Path path = Paths.get("data", "vehicles.txt");
        VehicleDatabase database = new VehicleDatabase(path);
        if (Files.notExists(path)) {
            database.putAll(Arrays.asList(VehicleFactory.readyVehicles()));
            log.info("Запись БД: создана с пятью демонстрационными объектами");
        } else {
            log.info("Чтение БД при старте: объектов=" + database.load());
        }
        return database;
    }
}
