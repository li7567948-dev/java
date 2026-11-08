package view;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Scanner;

import model.CheckResult;
import model.Event;
import model.Simulation;
import model.SolvabilityService;

/** Console-only view. It never modifies the model or handles exceptions. */
public final class ConsoleView {
    private final Scanner scanner;
    private final PrintStream output;

    public ConsoleView() {
        this(System.in, System.out);
    }

    public ConsoleView(InputStream input, PrintStream output) {
        if (input == null || output == null) {
            throw new IllegalArgumentException("Консольные потоки не заданы");
        }
        this.scanner = new Scanner(input, StandardCharsets.UTF_8.name());
        this.output = output;
    }

    public void showMenu() {
        output.println();
        output.println("========== ПУНКТ ПРОПУСКА ==========");
        output.println("1 — Проверка готовых машин (ЛР2)");
        output.println("2 — Проверить свою машину (ЛР2)");
        output.println("3 — Моделирование очереди (ЛР2)");
        output.println("4 — Коллекции и 10 экспериментов (ЛР3)");
        output.println("5 — Режим разрешимости (ЛР3)");
        output.println("6 — Исследование ёмкости ArrayList (ЛР3)");
        output.println("0 — Выход");
    }

    /** A single line of user input; parse/validation belong to the controller. */
    public String readLine(String prompt) {
        output.print(prompt);
        if (!scanner.hasNextLine()) throw new NoSuchElementException("Ввод завершён");
        return scanner.nextLine().trim();
    }

    public void showMessage(String message) {
        output.println(message);
    }

    public void showCheckResult(CheckResult result) {
        output.println(result.getVehicle());
        output.println("Результат: " + (result.isAllowed() ? "разрешено" : "запрещено"));
        if (!result.isAllowed()) output.println("Причина: " + result.getReason());
        output.println("Время проверки: " + result.getCheckTime() + " мин");
    }

    public void showSimulation(Simulation simulation) {
        output.println("===== РЕЗУЛЬТАТЫ =====");
        output.println("Время: " + simulation.getTime());
        output.println("Проверено: " + simulation.getCompleted());
        output.println("Разрешено: " + simulation.getAllowed());
        output.println("Запрещено: " + simulation.getDenied());
        output.println("В очереди: " + simulation.getQueueSize());
        output.println("На проверке: " + simulation.getActiveCount());
        output.println("===== СОБЫТИЯ =====");
        for (Event event : simulation.getEvents()) output.println(event);
    }

    public void showSolvability(SolvabilityService.Result result) {
        output.println("===== РЕЖИМ РАЗРЕШИМОСТИ =====");
        output.println("Дедлайн: " + result.getDeadlineMinutes() + " мин");
        output.println("До изменений: " + result.getInitialMinutes() + " мин");
        output.println("Возможное время: " + result.getProposedMinutes() + " мин");
        output.println("Активных пунктов в решении: " + result.getProposedPoints());
        output.println("Успевают пройти проверку: " + (result.isFeasible() ? "да" : "нет"));
        output.println(result.getExplanation());
        output.println("Примечание: успешная проверка по времени не означает разрешение на проезд.");
    }
}
