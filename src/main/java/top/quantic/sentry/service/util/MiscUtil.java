package top.quantic.sentry.service.util;

import static top.quantic.sentry.service.util.Inflection.pluralize;
import static top.quantic.sentry.service.util.Inflection.singularize;

public class MiscUtil {

    public static String humanizeBytes(long bytes) {
        int unit = 1000; // 1024 for non-SI units
        if (bytes < unit)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), "kMGTPE".charAt(exp - 1));
    }

    public static String inflect(long value, String label) {
        return value + " " + (value == 1 ? singularize(label) : pluralize(label));
    }

    private MiscUtil() {}
}
