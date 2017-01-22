package top.quantic.sentry.service.util;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Utility class to create maps. Usage pattern:
 * <pre>
 *
 * Collections.unmodifiableMap(Stream.of(
 *              entry(0, "zero"),
 *              entry(1, "one"),
 *              entry(2, "two"),
 *              entry(3, "three"),
 *              entry(4, "four"),
 *              entry(5, "five"),
 *              entry(6, "six"),
 *              entry(7, "seven"),
 *              entry(8, "eight"),
 *              entry(9, "nine"),
 *              entry(10, "ten"),
 *              entry(11, "eleven"),
 *              entry(12, "twelve"))
 *                  .collect(entriesToMap()));
 * </pre>
 */
public class Maps {

    public static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    public static <K, U> Collector<Map.Entry<K, U>, ?, Map<K, U>> entriesToMap() {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    public static <K, U> Collector<Map.Entry<K, U>, ?, ConcurrentMap<K, U>> entriesToConcurrentMap() {
        return Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    private Maps() {

    }
}
