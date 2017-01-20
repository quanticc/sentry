package top.quantic.sentry.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.quantic.sentry.domain.Setting;

import java.util.function.Function;

public class Key<T> {

    private static final Logger log = LoggerFactory.getLogger(Key.class);

    // GameServer
    public static final Key<Integer> RCON_SAY_INTERVAL_MINUTES = key("gameServer", "rconSayIntervalMinutes", 20);
    public static final Key<Integer> UPDATE_ATTEMPTS_ALERT_THRESHOLD = key("gameServer", "updateAttemptsAlertThreshold", 5);
    public static final Key<Integer> UPDATE_ATTEMPTS_INTERVAL_MINUTES = key("gameServer", "updateAttemptIntervalMinutes", 5);
    public static final Key<Integer> PING_ALERT_THRESHOLD = key("gameServer", "pingAlertThreshold", 1000);
    public static final Key<Integer> CONSECUTIVE_FAILURES_TO_TRIGGER = key("gameServer", "consecutiveFailuresToTrigger", 2);
    public static final Key<Integer> CONSECUTIVE_SUCCESSES_TO_RECOVER = key("gameServer", "consecutiveSuccessesToRecover", 2);

    // GameQuery
    public static final Key<Integer> CACHE_REFRESH_INTERVAL_MINUTES = key("sourceQuery", "cacheRefreshIntervalMinutes", 10);
    public static final Key<Integer> CACHE_EXPIRATION_INTERVAL_MINUTES = key("sourceQuery", "cacheExpirationIntervalMinutes", 15);
    public static final Key<Integer> VERSION_CACHE_EXPIRATION_MINUTES = key("steamWebApi", "versionCacheExpirationMinutes", 1);

    private final String group;
    private final String name;
    private final T defaultValue;
    private final Function<String, T> valueMapper;

    public static Key<Integer> key(String group, String name, Integer defaultValue) {
        return new Key<>(group, name, defaultValue, Key::parseInt);
    }

    public static Key<Long> key(String group, String name, Long defaultValue) {
        return new Key<>(group, name, defaultValue, Key::parseLong);
    }

    private Key(String group, String name, T defaultValue, Function<String, T> valueMapper) {
        this.group = group;
        this.name = name;
        this.defaultValue = defaultValue;
        this.valueMapper = valueMapper;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public T fromSetting(Setting setting) {
        if (!group.equals(setting.getGuild()) || !name.equals(setting.getKey())) {
            throw new IllegalArgumentException("Incompatible pair: " + setting.toString() + " with " + toString());
        }
        return valueMapper.apply(setting.getValue());
    }

    public T fromValue(String value) {
        return valueMapper.apply(value);
    }

    private static Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid format: {}", value, e);
        }
        return null;
    }

    private static Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid format: {}", value, e);
        }
        return null;
    }

    @Override
    public String toString() {
        return "Key{" +
            "group='" + group + '\'' +
            ", name='" + name + '\'' +
            ", defaultValue=" + defaultValue +
            '}';
    }
}
