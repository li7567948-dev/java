# Логи эксперимента

Запуск `java -cp bin controller.Main --benchmark` создаёт 10 файлов:

- `ArrayList_10.log`, `ArrayList_100.log`, `ArrayList_1000.log`, `ArrayList_10000.log`, `ArrayList_100000.log`
- `HashMap_10.log`, `HashMap_100.log`, `HashMap_1000.log`, `HashMap_10000.log`, `HashMap_100000.log`

Каждая операция зафиксирована отдельно с ID и временем в наносекундах. Внизу файла — `TotalCount`, `TotalTime`, `AverageTime`, `MedianTime`, а также общие суммы. Все десять логов включены в ZIP выдачи. В Git исходников хранится код воспроизведения и архив `results/10_performance_logs.zip` с полными проверенными логами; большие `*.log` по отдельности не коммитятся.
