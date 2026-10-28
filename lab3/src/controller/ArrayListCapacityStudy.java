package controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Task 6: compare operation-time spikes near old and modern capacity rules. */
public final class ArrayListCapacityStudy {
    private final ErrMsgLog logger;

    public ArrayListCapacityStudy(ErrMsgLog logger) { this.logger = logger; }

    public void run() throws IOException {
        // 10 000 measurements are sufficient to inspect early reallocations.
        List<String> lines = Files.readAllLines(logger.getDirectory().resolve("ArrayList_10000.log"),
                StandardCharsets.UTF_8);
        List<Long> times = new ArrayList<Long>();
        for (String line : lines) {
            if (line.startsWith("add, ID = ")) {
                int lastComma = line.lastIndexOf(',');
                times.add(Long.parseLong(line.substring(lastComma + 1).trim()));
            }
        }
        StringBuilder result = new StringBuilder();
        result.append("ЛР №3, пункт 6. Проверка предполагаемых расширений ArrayList\n");
        result.append("Времена взяты из реального файла ArrayList_10000.log. Единица: нс.\n");
        result.append("Индекс = число добавленных элементов ДО очередной операции add.\n\n");
        result.append("Старая формула из задания: (oldCapacity * 3) / 2 + 1\n");
        result.append("Современная приблизительная схема: new = old + old/2\n");
        result.append("ВАЖНО: ArrayList не предоставляет публичный getter для capacity.\n");
        result.append("Время операции само по себе не доказывает значение внутренней ёмкости.\n");
        result.append("На время влияют JIT-компиляция, GC и планирование потоков.\n\n");
        result.append("Гипотеза: старая формула\n");
        appendCapacitySeries(result, times, true);
        result.append("\nГипотеза: рост примерно в 1,5 раза\n");
        appendCapacitySeries(result, times, false);
        logger.analysisLog("ArrayList_capacity_check.txt", result.toString());
        logger.info("Анализ роста ArrayList создан: ArrayList_capacity_check.txt");
    }

    private void appendCapacitySeries(StringBuilder out, List<Long> durations, boolean oldFormula) {
        int capacity = 10;
        for (int i = 0; i < 12 && capacity < durations.size(); i++) {
            long actual = durations.get(capacity);
            long normal = (durations.get(capacity - 1) + durations.get(capacity + 1)) / 2;
            out.append("capacityHypothesis=").append(capacity)
                    .append("; measuredAddTime=").append(actual)
                    .append("; meanNeighbors=").append(normal)
                    .append("; ratio=")
                    .append(String.format(java.util.Locale.US, "%.2f", (double) actual / Math.max(1, normal)))
                    .append("\n");
            int newCapacity = oldFormula ? capacity * 3 / 2 + 1 : capacity + capacity / 2;
            if (newCapacity <= capacity) break;
            capacity = newCapacity;
        }
    }
}
