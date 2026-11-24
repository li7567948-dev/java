package view;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

import model.CheckResult;
import model.Event;
import model.Simulation;
import model.Vehicle;

/** Отображает меню и результаты; вводит данные, не выполняя расчёты модели. */
public final class ConsoleView implements AutoCloseable {
    private final Scanner scanner;
    private final PrintStream output;

    public ConsoleView(InputStream input, PrintStream output) {
        scanner = new Scanner(input, "UTF-8");
        this.output = output;
    }

    public void showMessage(String message) {
        output.println(message);
    }

    public void showError(Exception exception) {
        showMessage("Ошибка: " + exception.getMessage());
    }

    public String readString(String prompt) {
        output.print(prompt);
        output.flush();
        if (!scanner.hasNextLine()) {
            throw new NoSuchElementException("Конец ввода");
        }
        return scanner.nextLine().trim();
    }

    public int readInt(String prompt) {
        return Integer.parseInt(readString(prompt));
    }

    public long readId(String prompt) {
        long id = Long.parseLong(readString(prompt));
        if (id <= 0) {
            throw new IllegalArgumentException("ID должен быть положительным");
        }
        return id;
    }

    public double readDouble(String prompt) {
        return Double.parseDouble(readString(prompt).replace(',', '.'));
    }

    public void showMenu(boolean root) {
        showMessage("\n===== ПУНКТ ПРОПУСКА — ЛР 4 =====");
        showMessage("1 - Проверка готовых машин\n2 - Проверить свою машину\n3 - Моделирование очереди");
        showMessage("4 - Чтение базы данных\n5 - Запись базы данных\n6 - Добавление объектов");
        showMessage("7 - Изменение объекта\n8 - Удаление объекта\n9 - Список объектов\n10 - Поиск по ID");
        if (root) {
            showMessage("11 - Отладка\n12 - Автотесты");
        }
        showMessage("0 - Выход");
    }

    public void showVehicleTypes() {
        showMessage("1 - Легковой автомобиль\n2 - Грузовой автомобиль\n3 - Автобус\n4 - Трейлер\n5 - Автомобиль с прицепом");
    }

    public void showCargoTypes(String[] cargoTypes) {
        for (int index = 0; index < cargoTypes.length; index++) {
            showMessage((index + 1) + " - " + cargoTypes[index]);
        }
    }

    public void showSimulationMenu() {
        showMessage("1 - Три готовые машины\n2 - Случайный поток\n3 - Машины из БД");
    }

    public void showSimulationResult(Simulation simulation) {
        showMessage("Время: " + simulation.getTime() + "; проверено: " + simulation.getCompleted()
                + "; разрешено: " + simulation.getAllowed() + "; запрещено: " + simulation.getDenied()
                + "; в очереди: " + simulation.getQueueSize() + "; на проверке: " + simulation.getActiveCount());
        for (Event event : simulation.getEvents()) {
            showMessage(event.toString());
        }
    }

    public void showVehicle(Vehicle vehicle) {
        showMessage(vehicle.toString());
    }

    public void showCheckResult(CheckResult result) {
        showVehicle(result.getVehicle());
        showMessage("Результат: " + (result.isAllowed() ? "разрешено" : "запрещено"));
        if (!result.isAllowed()) {
            showMessage("Причина: " + result.getReason());
        }
        showMessage("Время проверки: " + result.getCheckTime() + " мин");
    }

    public void showVehicles(Map<Long, Vehicle> vehicles) {
        showMessage("Количество объектов: " + vehicles.size());
        for (Map.Entry<Long, Vehicle> entry : vehicles.entrySet()) {
            showMessage("ID=" + entry.getKey() + " | " + entry.getValue());
        }
    }

    @Override
    public void close() {
        scanner.close();
    }
}
