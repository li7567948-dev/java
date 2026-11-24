package model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Текстовая БД. Изменения сохраняются только после успешной записи файла. */
public final class VehicleDatabase {
    private final Path file;

    private Map<Long, Vehicle> vehicles = new LinkedHashMap<>();

    private long nextId = 1;

    public VehicleDatabase(Path file) {
        this.file = file.toAbsolutePath();
    }

    public Map<Long, Vehicle> getAll() {
        Map<Long, Vehicle> result = new LinkedHashMap<>();
        vehicles.forEach((id, vehicle) -> result.put(id, VehicleFactory.copyOf(vehicle)));
        return Collections.unmodifiableMap(result);
    }

    public Vehicle get(long id) {
        return VehicleFactory.copyOf(require(id));
    }

    private Vehicle require(long id) {
        Vehicle vehicle = vehicles.get(id);
        if (vehicle == null) {
            throw new IllegalArgumentException("ID не найден: " + id);
        }
        return vehicle;
    }

    public long put(Vehicle vehicle) throws IOException {
        return putAll(Collections.singletonList(vehicle)).get(0);
    }

    /**
     * Добавляет пакет объектов одной операцией записи.
     * @param items новые объекты
     * @return назначенные уникальные идентификаторы
     * @throws IOException при ошибке сохранения
     */
    public List<Long> putAll(List<Vehicle> items) throws IOException {
        Map<Long, Vehicle> snapshot = new LinkedHashMap<>(vehicles);
        List<Long> ids = new ArrayList<>();
        long nextIdentifier = nextId;
        for (Vehicle vehicle : items) {
            if (nextIdentifier == Long.MAX_VALUE) {
                throw new IllegalStateException("Исчерпаны ID");
            }
            snapshot.put(nextIdentifier, VehicleFactory.copyOf(vehicle));
            ids.add(nextIdentifier++);
        }
        commit(snapshot, nextIdentifier);
        return ids;
    }

    public void change(long id, Vehicle vehicle) throws IOException {
        require(id);
        Map<Long, Vehicle> snapshot = new LinkedHashMap<>(vehicles);
        snapshot.put(id, VehicleFactory.copyOf(vehicle));
        commit(snapshot, nextId);
    }

    public void del(long id) throws IOException {
        require(id);
        Map<Long, Vehicle> snapshot = new LinkedHashMap<>(vehicles);
        snapshot.remove(id);
        commit(snapshot, nextId);
    }

    public void save() throws IOException {
        commit(vehicles, nextId);
    }

    /** Сначала записывает временный файл, затем заменяет исходный файл. */
    private void commit(Map<Long, Vehicle> snapshot, long nextIdentifier) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporaryFile = Files.createTempFile(file.getParent(), "vehicles-", ".tmp");
        try {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(temporaryFile.toFile()), StandardCharsets.UTF_8))) {
                writer.write("VEHICLES_V1\t" + nextIdentifier);
                writer.newLine();
                for (Map.Entry<Long, Vehicle> entry : snapshot.entrySet()) {
                    Vehicle vehicle = entry.getValue();
                    writer.write(entry.getKey() + "\t" + VehicleFactory.typeOf(vehicle) + "\t" + vehicle.getNumber()
                            + "\t" + vehicle.getLength() + "\t" + vehicle.getWidth() + "\t" + vehicle.getHeight()
                            + "\t" + vehicle.getWeight() + "\t" + vehicle.getSpeed() + "\t" + vehicle.getCargo()
                            + "\t" + vehicle.getArriveTime());
                    writer.newLine();
                }
            }
            try {
                Files.move(temporaryFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
            vehicles = snapshot;
            nextId = nextIdentifier;
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /**
     * Загружает все записи во временную коллекцию и проверяет уникальность ID.
     * @return количество прочитанных объектов
     * @throws IOException если файл недоступен или содержит ошибочные данные
     */
    public int load() throws IOException {
        Map<Long, Vehicle> snapshot = new LinkedHashMap<>();
        long nextIdentifier;
        int lineNumber = 1;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file.toFile()), StandardCharsets.UTF_8.newDecoder()))) {
            String header = reader.readLine();
            if (header == null || !header.startsWith("VEHICLES_V1\t")) {
                throw new IOException("Неверный заголовок БД");
            }
            nextIdentifier = Long.parseLong(header.substring("VEHICLES_V1\t".length()));
            if (nextIdentifier < 1) {
                throw new IllegalArgumentException("Неверный следующий ID");
            }
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] fields = line.split("\t", -1);
                if (fields.length != 10) {
                    throw new IllegalArgumentException("Ожидается 10 полей");
                }
                long id = Long.parseLong(fields[0]);
                if (id < 1 || id >= nextIdentifier || snapshot.containsKey(id)) {
                    throw new IllegalArgumentException("Неверный или повторный ID");
                }
                Vehicle vehicle = VehicleFactory.create(Integer.parseInt(fields[1]), fields[2], Double.parseDouble(fields[3]),
                        Double.parseDouble(fields[4]), Double.parseDouble(fields[5]), Double.parseDouble(fields[6]),
                        Integer.parseInt(fields[7]), fields[8]);
                vehicle.setArriveTime(Integer.parseInt(fields[9]));
                snapshot.put(id, vehicle);
            }
        } catch (IllegalArgumentException exception) {
            throw new IOException("Ошибка БД, строка " + lineNumber + ": " + exception.getMessage(), exception);
        }
        vehicles = snapshot;
        nextId = nextIdentifier;
        return vehicles.size();
    }
}
