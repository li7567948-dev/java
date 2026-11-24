package model;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;

/** Загрузка настроек перед входом в систему. */
public final class AppConfig {

    public enum Role {
        ROOT, USER
    }
    private final String login, password;
    private final Role role;
    private final boolean debug, autotests;

    private AppConfig(Properties properties) {
        login = required(properties, "login");
        password = required(properties, "password");
        String group = required(properties, "group");
        if ("root".equals(group)) {
            role = Role.ROOT;
        } else if ("user".equals(group)) {
            role = Role.USER;
        } else {
            throw new IllegalArgumentException("group: root или user");
        }
        debug = flag(properties, "debug");
        autotests = flag(properties, "autotests");
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Нет настройки: " + key);
        }
        return value.trim();
    }

    private static boolean flag(Properties properties, String key) {
        String value = required(properties, key);
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new IllegalArgumentException(key + ": true или false");
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * Читает обязательные параметры в UTF-8.
     * @param path путь к файлу настроек
     * @return проверенная конфигурация
     * @throws IOException если файл недоступен
     * @throws IllegalArgumentException если настройки некорректны
     */
    public static AppConfig load(Path path) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(new FileInputStream(path.toFile()), StandardCharsets.UTF_8.newDecoder())) {
            properties.load(reader);
        }
        return new AppConfig(properties);
    }

    public boolean authenticate(String enteredLogin, String enteredPassword) {
        return login.equals(enteredLogin) && password.equals(enteredPassword);
    }

    public String getLogin() {
        return login;
    }

    public Role getRole() {
        return role;
    }

    public boolean isRoot() {
        return role == Role.ROOT;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isAutotests() {
        return autotests;
    }
}
