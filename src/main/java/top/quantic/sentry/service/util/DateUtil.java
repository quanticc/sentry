package top.quantic.sentry.service.util;

import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DateUtil {

    public static String humanize(Duration duration) {
        return humanize(duration, false);
    }

    public static String humanize(Duration duration, boolean minimal) {
        Duration abs = duration.abs();
        long totalSeconds = abs.getSeconds();
        if (totalSeconds == 0) {
            return abs.toMillis() + (minimal ? "ms" : " milliseconds");
        }
        long d = totalSeconds / (3600 * 24);
        long h = (totalSeconds % (3600 * 24)) / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        String days = minimal ? compact(d, "d") : inflect(d, "day");
        String hours = minimal ? compact(h, "h") : inflect(h, "hour");
        String minutes = minimal ? compact(m, "m") : inflect(m, "minute");
        String seconds = minimal ? compact(s, "s") : inflect(s, "second");
        return Stream.of(days, hours, minutes, seconds)
            .filter(str -> !str.isEmpty()).collect(Collectors.joining(minimal ? "" : ", "));
    }

    private static String compact(long value, String suffix) {
        return (value == 0 ? "" : value + suffix);
    }

    private static String inflect(long value, String singular) {
        return (value == 1 ? "1 " + singular : (value > 1 ? value + " " + singular + "s" : ""));
    }

    private DateUtil() {

    }
}
